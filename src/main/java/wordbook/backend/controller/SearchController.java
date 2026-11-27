package wordbook.backend.controller;

import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import wordbook.backend.api.service.ApiService;
import wordbook.backend.domain.user.service.UserService;
import wordbook.backend.domain.word.dto.WordResponseDTO;

import wordbook.backend.domain.word.dto.WordSearchRequestDTO;
import wordbook.backend.domain.word.service.WordService;
import wordbook.backend.error.api.AIUnavailableException;
import wordbook.backend.error.api.InvalidInputException;
import wordbook.backend.error.api.UsageExhaustedException;
import wordbook.backend.search.SearchService;
import wordbook.backend.usage.UsageService;

import java.util.regex.Pattern;

@Slf4j
@RestController

@RequestMapping("/api/v2/openai")
public class SearchController {
    //멱등성 키는 UUID 형식만 허용한다. 값이 Redis 키(idem:<user>:<key>)에 그대로 들어가므로 길이·문자를 제한한다
    private static final Pattern UUID_PATTERN =
            Pattern.compile("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$");
    private final SearchService searchService;

    public SearchController(SearchService searchService) {
        this.searchService = searchService;
    }
    //예문 검색
    @PostMapping("/search")
    public ResponseEntity<WordResponseDTO> search(
            Authentication authentication,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody WordSearchRequestDTO wordSearchRequestDTO
            )
    {
        String username = authentication.getName();
        validateIdempotencyKey(idempotencyKey);
        WordResponseDTO wordResponseDTO=searchService.search(username,idempotencyKey.toLowerCase(),wordSearchRequestDTO);
        return ResponseEntity.ok(wordResponseDTO);

    }
    private void validateIdempotencyKey(String idempotencyKey){
        if(!UUID_PATTERN.matcher(idempotencyKey).matches()){
            throw new InvalidInputException();
        }
    }
}
