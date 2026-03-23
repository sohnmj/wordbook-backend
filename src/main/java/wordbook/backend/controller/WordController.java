package wordbook.backend.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import wordbook.backend.domain.word.dto.WordResponseDTO;
import wordbook.backend.domain.word.service.WordService;

import java.util.List;

@RestController
@RequestMapping("api/v2/word")
public class WordController {
    private final WordService wordService;

    public WordController(WordService wordService) {
        this.wordService = wordService;
    }
    @GetMapping("/list")
    public ResponseEntity<List<WordResponseDTO>> getWordList(Authentication authentication) {
        String username = authentication.getName();
        List<WordResponseDTO> wordList = wordService.getWordList(username);
        return ResponseEntity.ok(wordList);
    }
}
