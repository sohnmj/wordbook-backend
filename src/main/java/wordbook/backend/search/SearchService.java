package wordbook.backend.search;

import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;
import wordbook.backend.api.service.ApiService;
import wordbook.backend.domain.word.dto.WordResponseDTO;
import wordbook.backend.domain.word.dto.WordSearchRequestDTO;
import wordbook.backend.domain.word.service.WordService;
import wordbook.backend.error.api.RequestInProgressException;
import wordbook.backend.error.api.TemporarilyUnavailableException;
import wordbook.backend.redis.IdempotencyResult;
import wordbook.backend.redis.RedisService;
import wordbook.backend.usage.UsageService;

@Service
@Slf4j
public class SearchService {
    private static final String IDEMPOTENCY_KEY_PREFIX = "idem:";

    private final ApiService apiService;
    private final WordService wordService;
    private final UsageService usageService;
    private final RedisService redisService;
    private final ObjectMapper objectMapper;

    public SearchService(ApiService apiService, WordService wordService, UsageService usageService,
                         RedisService redisService, ObjectMapper objectMapper) {
        this.apiService = apiService;
        this.wordService = wordService;
        this.usageService = usageService;
        this.redisService = redisService;
        this.objectMapper = objectMapper;
    }

    public WordResponseDTO search(String username, String idempotencyKey, WordSearchRequestDTO dto) {
        String key = IDEMPOTENCY_KEY_PREFIX + username + ":" + idempotencyKey;
        IdempotencyResult result = getIdempotencyResult(key);
        switch (result.status()) {
            case DONE -> {
                try {
                    return objectMapper.readValue(result.responseJson(), WordResponseDTO.class);
                } catch (RuntimeException ex) {
                    throw new TemporarilyUnavailableException(ex);
                }
            }
            case IN_PROGRESS -> throw new RequestInProgressException();
            case STARTED -> { }
        }

        try {
            usageService.decreaseUsage(username);
        } catch (RuntimeException ex) {
            releaseQuietly(key);
            throw ex;
        }

        WordResponseDTO saved;
        try {
            WordResponseDTO response = apiService.getResponseEX(dto);
            saved = wordService.createWord(response, username);
        } catch (RuntimeException ex) {
            refundQuietly(username, ex);
            releaseQuietly(key);
            throw ex;
        }

        markDoneQuietly(key, saved);
        return saved;
    }

    //Redis 장애 시 fail-closed: 차감·AI 호출 전에 503으로 막는다
    private IdempotencyResult getIdempotencyResult(String key) {
        try {
            return redisService.tryStart(key);
        } catch (DataAccessException ex) {
            throw new TemporarilyUnavailableException(ex);
        }
    }

    private void refundQuietly(String username, RuntimeException ex) {
        try {
            usageService.refundUsage(username);
        } catch (RuntimeException refundEx) {
            log.error("사용량 환불 실패 username={}", username, refundEx);
            ex.addSuppressed(refundEx);
        }
    }

    private void releaseQuietly(String key) {
        try {
            redisService.releaseIdempotency(key);
        } catch (RuntimeException rex) {
            log.warn("멱등성 키 삭제 실패 key={}", key, rex);
        }
    }

    private void markDoneQuietly(String key, WordResponseDTO saved) {
        try {
            redisService.markIdempotencyDone(key, objectMapper.writeValueAsString(saved));
        } catch (RuntimeException ex) {
            log.warn("멱등성 DONE 기록 실패 key={}", key, ex);
        }
    }
}
