package wordbook.backend.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import wordbook.backend.api.ApiResponseDTO;
import wordbook.backend.api.service.ApiService;
import wordbook.backend.domain.user.service.UserService;
import wordbook.backend.domain.word.dto.WordResponseDTO;

import wordbook.backend.domain.word.service.WordService;
import wordbook.backend.usage.UsageService;

@Slf4j
@RestController
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
    @GetMapping("/search")
    public ResponseEntity<WordResponseDTO> search(Authentication authentication, @RequestParam String keyword, @RequestParam String lang,@RequestParam String topic,@RequestParam String meaning) {
        String username = authentication.getName();
        log.info("username:{}",username);
        //usage 없으면 에러 던지기
        if(!userService.possibleUsage(username)){
            throw new RuntimeException();
        }
        WordResponseDTO response = apiService.getResponseEX(keyword, lang,meaning,topic);
        WordResponseDTO wordResponseDTO = wordService.createWord( response, username);
        usageService.decreaseUsage(username);
        return ResponseEntity.ok(wordResponseDTO);
    }
}
