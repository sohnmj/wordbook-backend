package wordbook.backend.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import wordbook.backend.api.ApiResponseDTO;
import wordbook.backend.api.service.ApiService;
import wordbook.backend.domain.word.dto.WordResponseDTO;

import wordbook.backend.domain.word.service.WordService;

@RestController
public class SearchController {
    private final ApiService apiService;
    private final WordService wordService;
    public SearchController(ApiService apiService, WordService wordService) {
        this.apiService = apiService;
        this.wordService = wordService;
    }
    @GetMapping("/search")
    public ResponseEntity<WordResponseDTO> search(Authentication authentication, @RequestParam String keyword, @RequestParam String lang) {
        String username = authentication.getName();
        WordResponseDTO response = apiService.getResponse(keyword, lang);
        WordResponseDTO wordResponseDTO = wordService.createWord( response, username);
        return ResponseEntity.ok(wordResponseDTO);
    }
}
