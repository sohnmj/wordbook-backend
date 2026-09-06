package wordbook.backend.controller;

import jakarta.validation.constraints.NotBlank;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import wordbook.backend.api.ApiResponseDTO;
import wordbook.backend.api.service.ApiService;
import wordbook.backend.domain.user.service.UserService;
import wordbook.backend.domain.word.dto.WordResponseDTO;

import wordbook.backend.domain.word.service.WordService;
import wordbook.backend.usage.UsageService;

@Slf4j
@RestController
@Validated
@RequestMapping("/api/v2/openai")
public class SearchController {
    private final ApiService apiService;
    private final WordService wordService;
    private final UserService userService;
    private final UsageService usageService;

    public SearchController(ApiService apiService, WordService wordService, UserService userService, UsageService usageService) {
        this.apiService = apiService;
        this.wordService = wordService;
        this.userService = userService;
        this.usageService = usageService;
    }
    //예문 검색
    @GetMapping("/search")
    public ResponseEntity<WordResponseDTO> search(
            Authentication authentication,
            @NotBlank(message = "no keyword") @RequestParam String keyword,
            @NotBlank(message = "no lang") @RequestParam String lang,
            @RequestParam(required = false) String topic,
            @RequestParam(required = false) String meaning
    )
    {    String username = authentication.getName();
        log.info("username:{}",username);
        //usage 없으면 에러 던지기
        if(!userService.possibleUsage(username)){
            throw new IllegalStateException("일일 이용 횟수를 모두 소진하였습니다.");
        }
        WordResponseDTO response = apiService.getResponseEX(keyword, lang,meaning,topic);
        WordResponseDTO wordResponseDTO = wordService.createWord( response, username);
        usageService.decreaseUsage(username);
        return ResponseEntity.ok(wordResponseDTO);
    }
}
