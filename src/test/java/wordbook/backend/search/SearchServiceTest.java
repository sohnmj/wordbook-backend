package wordbook.backend.search;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.RedisConnectionFailureException;
import tools.jackson.databind.json.JsonMapper;
import wordbook.backend.api.service.ApiService;
import wordbook.backend.domain.word.dto.WordResponseDTO;
import wordbook.backend.domain.word.dto.WordSearchRequestDTO;
import wordbook.backend.domain.word.service.WordService;
import wordbook.backend.error.ErrorCode;
import wordbook.backend.error.api.AIUnavailableException;
import wordbook.backend.error.api.RequestInProgressException;
import wordbook.backend.error.api.TemporarilyUnavailableException;
import wordbook.backend.error.api.UsageExhaustedException;
import wordbook.backend.redis.IdempotencyResult;
import wordbook.backend.redis.RedisService;
import wordbook.backend.usage.UsageService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

//검색 흐름(멱등성 키 → 선차감 → AI 호출 → 저장 → DONE 기록)과 실패 시 환불·키 삭제를 검증한다
@ExtendWith(MockitoExtension.class)
class SearchServiceTest {

    private static final String USER = "testUser";
    private static final String IDEM_KEY = "3f2b8c1e-9a4d-4e6f-8b2a-1c3d5e7f9a0b";
    private static final String REDIS_KEY = "idem:" + USER + ":" + IDEM_KEY;

    @Mock
    private ApiService apiService;
    @Mock
    private WordService wordService;
    @Mock
    private UsageService usageService;
    @Mock
    private RedisService redisService;

    //JSON 직렬화·역직렬화는 실제 매퍼로 확인한다
    private final JsonMapper objectMapper = JsonMapper.builder().build();
    private SearchService searchService;

    private final WordSearchRequestDTO request = new WordSearchRequestDTO("apple", "en", null, null);
    private final WordResponseDTO aiResponse = WordResponseDTO.builder().word("apple").example("I ate an apple.").build();
    private final WordResponseDTO saved = WordResponseDTO.builder().wordId(1L).word("apple").example("I ate an apple.").build();

    @BeforeEach
    void setUp() {
        searchService = new SearchService(apiService, wordService, usageService, redisService, objectMapper);
    }

    private void 처음_오는_키() {
        given(redisService.tryStart(REDIS_KEY)).willReturn(IdempotencyResult.started());
    }

    private void AI호출과_저장이_성공한다() {
        given(apiService.getResponseEX(request)).willReturn(aiResponse);
        given(wordService.createWord(aiResponse, USER)).willReturn(saved);
    }

    private WordResponseDTO 검색() {
        return searchService.search(USER, IDEM_KEY, request);
    }

    // ===== 성공 =====

    @Test
    void 처음_오는_키로_성공하면_저장_결과를_반환하고_DONE을_기록한다() {
        처음_오는_키();
        AI호출과_저장이_성공한다();

        WordResponseDTO result = 검색();

        assertThat(result).isEqualTo(saved);
        verify(redisService).markIdempotencyDone(eq(REDIS_KEY), anyString());
        verify(usageService, never()).refundUsage(anyString());
        verify(redisService, never()).releaseIdempotency(anyString());
    }

    @Test
    void 성공하면_키등록_차감_AI호출_저장_DONE기록_순서로_실행된다() {
        처음_오는_키();
        AI호출과_저장이_성공한다();

        검색();

        InOrder order = inOrder(redisService, usageService, apiService, wordService);
        order.verify(redisService).tryStart(REDIS_KEY);
        order.verify(usageService).decreaseUsage(USER);
        order.verify(apiService).getResponseEX(request);
        order.verify(wordService).createWord(aiResponse, USER);
        order.verify(redisService).markIdempotencyDone(eq(REDIS_KEY), anyString());
    }

    @Test
    void DONE에는_저장_결과를_JSON으로_기록한다() {
        처음_오는_키();
        AI호출과_저장이_성공한다();

        검색();

        verify(redisService).markIdempotencyDone(REDIS_KEY, objectMapper.writeValueAsString(saved));
    }

    @Test
    void Redis_키는_idem_사용자명_멱등성키_형식이다() {
        처음_오는_키();
        AI호출과_저장이_성공한다();

        검색();

        verify(redisService).tryStart("idem:testUser:" + IDEM_KEY);
    }

    // ===== 이미 처리됐거나 처리 중인 키 =====

    @Test
    void 이미_DONE인_키면_저장된_응답을_반환하고_다시_처리하지_않는다() {
        given(redisService.tryStart(REDIS_KEY))
                .willReturn(IdempotencyResult.done(objectMapper.writeValueAsString(saved)));

        WordResponseDTO result = 검색();

        assertThat(result).isEqualTo(saved);
        verify(usageService, never()).decreaseUsage(anyString());
        verify(apiService, never()).getResponseEX(any());
        verify(wordService, never()).createWord(any(), anyString());
        verify(redisService, never()).markIdempotencyDone(anyString(), anyString());
    }

    @Test
    void DONE인데_저장된_JSON이_깨졌으면_503을_던지고_다시_처리하지_않으며_키도_지우지_않는다() {
        given(redisService.tryStart(REDIS_KEY)).willReturn(IdempotencyResult.done("not-json"));

        assertThatThrownBy(this::검색)
                .isInstanceOf(TemporarilyUnavailableException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.TEMPORARILY_UNAVAILABLE);

        verify(usageService, never()).decreaseUsage(anyString());
        verify(apiService, never()).getResponseEX(any());
        // 결정 (b): 키는 남겨 두고, 프론트가 새 키로 재시도한다
        verify(redisService, never()).releaseIdempotency(anyString());
    }

    @Test
    void 처리_중인_키면_409를_던지고_다시_처리하지_않으며_키도_지우지_않는다() {
        given(redisService.tryStart(REDIS_KEY)).willReturn(IdempotencyResult.inProgress());

        assertThatThrownBy(this::검색)
                .isInstanceOf(RequestInProgressException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.REQUEST_IN_PROGRESS);

        verify(usageService, never()).decreaseUsage(anyString());
        verify(apiService, never()).getResponseEX(any());
        // 다른 요청이 처리 중인 키이므로 지우면 안 된다
        verify(redisService, never()).releaseIdempotency(anyString());
    }

    @Test
    void 키_등록_중_Redis가_장애면_503을_던지고_차감하지_않는다() {
        RedisConnectionFailureException redisDown = new RedisConnectionFailureException("Redis 연결 실패");
        given(redisService.tryStart(REDIS_KEY)).willThrow(redisDown);

        assertThatThrownBy(this::검색)
                .isInstanceOf(TemporarilyUnavailableException.class)
                .hasCause(redisDown)
                .extracting("errorCode").isEqualTo(ErrorCode.TEMPORARILY_UNAVAILABLE);

        verify(usageService, never()).decreaseUsage(anyString());
        verify(apiService, never()).getResponseEX(any());
    }

    // ===== 처리 중 실패 =====

    @Test
    void 사용량이_소진되면_429예외를_그대로_던지고_키를_지운다() {
        처음_오는_키();
        UsageExhaustedException exhausted = new UsageExhaustedException();
        willThrow(exhausted).given(usageService).decreaseUsage(USER);

        assertThatThrownBy(this::검색).isSameAs(exhausted);

        verify(redisService).releaseIdempotency(REDIS_KEY);
        verify(apiService, never()).getResponseEX(any());
        verify(usageService, never()).refundUsage(anyString());
    }

    @Test
    void AI호출이_실패하면_원래_예외를_던지고_환불하고_키를_지운다() {
        처음_오는_키();
        AIUnavailableException aiFail = new AIUnavailableException(new RuntimeException("OpenAI 장애"));
        given(apiService.getResponseEX(request)).willThrow(aiFail);

        assertThatThrownBy(this::검색).isSameAs(aiFail);

        verify(usageService).refundUsage(USER);
        verify(redisService).releaseIdempotency(REDIS_KEY);
        verify(wordService, never()).createWord(any(), anyString());
        verify(redisService, never()).markIdempotencyDone(anyString(), anyString());
    }

    @Test
    void 단어_저장이_실패하면_원래_예외를_던지고_환불하고_키를_지운다() {
        처음_오는_키();
        IllegalStateException saveFail = new IllegalStateException("저장 실패");
        given(apiService.getResponseEX(request)).willReturn(aiResponse);
        given(wordService.createWord(aiResponse, USER)).willThrow(saveFail);

        assertThatThrownBy(this::검색).isSameAs(saveFail);

        verify(usageService).refundUsage(USER);
        verify(redisService).releaseIdempotency(REDIS_KEY);
        verify(redisService, never()).markIdempotencyDone(anyString(), anyString());
    }

    @Test
    void 환불도_실패하면_원래_예외를_던지고_환불_예외는_suppressed로_붙으며_키는_지운다() {
        처음_오는_키();
        AIUnavailableException aiFail = new AIUnavailableException(new RuntimeException("OpenAI 장애"));
        IllegalStateException refundFail = new IllegalStateException("환불 실패");
        given(apiService.getResponseEX(request)).willThrow(aiFail);
        willThrow(refundFail).given(usageService).refundUsage(USER);

        assertThatThrownBy(this::검색)
                .isSameAs(aiFail)
                .satisfies(ex -> assertThat(ex.getSuppressed()).containsExactly(refundFail));

        verify(redisService).releaseIdempotency(REDIS_KEY);
    }

    @Test
    void 키_삭제도_실패하면_원래_예외가_그대로_전파된다() {
        처음_오는_키();
        AIUnavailableException aiFail = new AIUnavailableException(new RuntimeException("OpenAI 장애"));
        given(apiService.getResponseEX(request)).willThrow(aiFail);
        willThrow(new RedisConnectionFailureException("Redis 연결 실패"))
                .given(redisService).releaseIdempotency(REDIS_KEY);

        assertThatThrownBy(this::검색).isSameAs(aiFail);

        verify(usageService).refundUsage(USER);
    }

    @Test
    void DONE_기록이_실패해도_저장_결과를_정상_반환한다() {
        처음_오는_키();
        AI호출과_저장이_성공한다();
        willThrow(new RedisConnectionFailureException("Redis 연결 실패"))
                .given(redisService).markIdempotencyDone(eq(REDIS_KEY), anyString());

        WordResponseDTO result = 검색();

        // ADR-006: 이미 차감·저장이 끝났으므로 200으로 응답한다
        assertThat(result).isEqualTo(saved);
        verify(usageService, never()).refundUsage(anyString());
        verify(redisService, never()).releaseIdempotency(anyString());
    }
}
