package wordbook.backend.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import wordbook.backend.domain.word.dto.WordResponseDTO;
import wordbook.backend.domain.wordbookword.service.WordBookWordService;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v2/test")
public class WordBookTestController {
    private final  WordBookWordService wordBookWordService;
    @GetMapping("")
    public ResponseEntity<List<WordResponseDTO>> getTests(@RequestParam Long wordbookId, @RequestParam int size){
        List<WordResponseDTO> testWords = wordBookWordService.getTestWords(wordbookId, size);
        return ResponseEntity.ok(testWords);
    }

}