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
import wordbook.backend.error.api.UsageExhaustedException;
import wordbook.backend.usage.UsageService;

@Slf4j
@RestController

@RequestMapping("/api/v2/openai")
public class SearchController {
    private final ApiService apiService;
    private final WordService wordService;

    private final UsageService usageService;

    public SearchController(ApiService apiService, WordService wordService,  UsageService usageService) {
        this.apiService = apiService;
        this.wordService = wordService;
        this.usageService = usageService;
    }
    //예문 검색
    @PostMapping("/search")
    public ResponseEntity<WordResponseDTO> search(
            Authentication authentication,
            @Valid @RequestBody WordSearchRequestDTO wordSearchRequestDTO
            )
    {
        String username = authentication.getName();
        usageService.decreaseUsage(username);
        try {
            WordResponseDTO response = apiService.getResponseEX(wordSearchRequestDTO);
            WordResponseDTO wordResponseDTO = wordService.createWord(response, username);
            return ResponseEntity.ok(wordResponseDTO);
        }catch (RuntimeException ex){
            try {
                usageService.refundUsage(username);
            } catch (Exception e) {
                log.warn("{} 환불을 실패",username);
            }
            throw ex;
        }


    }
}
