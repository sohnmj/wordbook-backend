package wordbook.backend.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import wordbook.backend.domain.word.dto.WordResponseDTO;
import wordbook.backend.domain.wordbook.dto.WordBookRequestDTO;
import wordbook.backend.domain.wordbook.dto.WordBookListResponseDTO;
import wordbook.backend.domain.wordbook.dto.WordBookResponseDTO;
import wordbook.backend.domain.wordbook.service.WordBookService;
import wordbook.backend.domain.wordbookword.service.WordBookWordService;

import java.util.List;

@RestController
@RequestMapping("/api/v2/wordbook")
public class WordBookController {
    private final WordBookService wordBookService;
    private final WordBookWordService wordBookWordService;
    public WordBookController(WordBookService wordBookService, WordBookWordService wordBookWordService) {
        this.wordBookService = wordBookService;
        this.wordBookWordService = wordBookWordService;
    }
    @PostMapping("")
    public ResponseEntity<WordBookListResponseDTO> create(@RequestBody WordBookRequestDTO wordBookRequestDTO, Authentication authentication) {
        String username=authentication.getName();
        WordBookListResponseDTO save = wordBookService.save(wordBookRequestDTO, username);
        return ResponseEntity.ok(save);
    }
    @GetMapping("/list")
    public ResponseEntity<List<WordBookListResponseDTO>> list(Authentication authentication) {
        String username = authentication.getName();
        List<WordBookListResponseDTO> myAll = wordBookService.findMyAll(username);
        return ResponseEntity.ok(myAll);
    }
    @GetMapping("")
    public ResponseEntity<WordBookResponseDTO> getWordBook(@RequestParam long id) {

        WordBookResponseDTO wordBook = wordBookWordService.getWordBook(id);
        return ResponseEntity.ok(wordBook);
    }
    @GetMapping("/test")
    public ResponseEntity<List<WordResponseDTO>> createTest(@RequestParam long id,@RequestParam int size) {
        return ResponseEntity.ok(wordBookWordService.getTestWords(id,size));
    }
    @DeleteMapping("")
    public ResponseEntity<Long> delete(@RequestParam long id,Authentication authentication) {
        String username=authentication.getName();
        wordBookService.remove(id,username);
        return ResponseEntity.ok(id);
    }

}
