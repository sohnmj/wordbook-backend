package wordbook.backend.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import wordbook.backend.domain.wordbookword.dto.WordBookWordRequestDTO;
import wordbook.backend.domain.wordbookword.service.WordBookWordService;

@RestController
@RequestMapping("/api/v2/wordbookword")
public class WordBookWordController {
    private final WordBookWordService wordBookWordService;

    public WordBookWordController(WordBookWordService wordBookWordService) {
        this.wordBookWordService = wordBookWordService;
    }

    @PostMapping("")
    public ResponseEntity<Long> createWordBookWord(@RequestBody WordBookWordRequestDTO wordBookWordRequestDTO,Authentication authentication) {
        Long wordId=wordBookWordRequestDTO.getWord();
        Long wordBookId=wordBookWordRequestDTO.getWordbook();
        String username=authentication.getName();
        Long wordBookWord = wordBookWordService.createWordBookWord(wordId, wordBookId,username);
        return ResponseEntity.ok(wordBookWord);
    }
    @DeleteMapping("")
    public ResponseEntity<Long> deleteWordBookWord(@RequestParam Long wordBookId, @RequestParam Long wordId, Authentication authentication) {
        long id = wordBookWordService.removeWordBookWord(wordBookId, wordId, authentication.getName());
        return ResponseEntity.ok(id);

    }
}
