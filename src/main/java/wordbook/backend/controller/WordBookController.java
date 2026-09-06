package wordbook.backend.controller;

import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import wordbook.backend.domain.wordbook.dto.WordBookRequestDTO;
import wordbook.backend.domain.wordbook.dto.WordBookListResponseDTO;
import wordbook.backend.domain.wordbook.dto.WordBookResponseDTO;
import wordbook.backend.domain.wordbook.service.WordBookService;
import wordbook.backend.domain.wordbookword.service.WordBookWordService;

import java.util.List;
@Slf4j
@RestController
@RequestMapping("/api/v2/wordbook")
public class WordBookController {
    private final WordBookService wordBookService;
    private final WordBookWordService wordBookWordService;
    public WordBookController(WordBookService wordBookService, WordBookWordService wordBookWordService) {
        this.wordBookService = wordBookService;
        this.wordBookWordService = wordBookWordService;
    }
    // 단어장 생성 (매개변수 : 이름)
    @PostMapping("")
    public ResponseEntity<WordBookListResponseDTO> create(@RequestBody @Valid WordBookRequestDTO wordBookRequestDTO, Authentication authentication) {
        String username=authentication.getName();
        WordBookListResponseDTO save = wordBookService.save(wordBookRequestDTO, username);
        return ResponseEntity.ok(save);
    }
    // 사용자의 단어장 리스트 반환
    @GetMapping("/mylist")
    public ResponseEntity<List<WordBookListResponseDTO>> getOwnWordbookList(Authentication authentication) {

        String username = authentication.getName();
        List<WordBookListResponseDTO> myAll = wordBookService.findMyAll(username);
        return ResponseEntity.ok(myAll);
    }
    // 특정 아이디의 단어장 조회
    @GetMapping("")
    public ResponseEntity<WordBookResponseDTO> getWordBook(@RequestParam long id) {

        WordBookResponseDTO wordBook = wordBookWordService.getWordBook(id);
        return ResponseEntity.ok(wordBook);
    }

    //단어장 삭제
    @DeleteMapping("")
    public ResponseEntity<Long> delete(@RequestParam long id,Authentication authentication) {
        String username=authentication.getName();
        wordBookService.remove(id,username);
        return ResponseEntity.ok(id);
    }

    //단어장 (좋아요 순/시간순) n개 목록 반환
    @GetMapping("/list")
    public ResponseEntity<List<WordBookListResponseDTO>> getList(@PageableDefault(page = 0, size = 10, sort = "id", direction = Sort.Direction.DESC) Pageable pageable){
        List<WordBookListResponseDTO> wordbookList = wordBookService.getWordbookList(pageable);
        return ResponseEntity.ok(wordbookList);
    }


}
