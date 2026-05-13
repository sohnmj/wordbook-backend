package wordbook.backend.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import wordbook.backend.domain.wordbookcomment.dto.WordBookCommentRequestDTO;
import wordbook.backend.domain.wordbookcomment.dto.WordBookCommentResponseDTO;
import wordbook.backend.domain.wordbookcomment.entity.WordBookCommentEntity;
import wordbook.backend.domain.wordbookcomment.service.WordBookCommentService;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v2/wordbookcomment")
public class WordBookCommentController {
    private WordBookCommentService wordBookCommentService;
    @PostMapping("")
    public Long createParentComment(@RequestBody WordBookCommentRequestDTO wordBookCommentRequestDTO, Authentication authentication) {
        String username=authentication.getName();
        wordBookCommentRequestDTO.setUsername(username);
        return wordBookCommentService.createComment(wordBookCommentRequestDTO);

    }
    @GetMapping("")
    public ResponseEntity<List<WordBookCommentResponseDTO>> getComments(@RequestParam Long wordbookId) {
        List<WordBookCommentResponseDTO> comments = wordBookCommentService.getComments(wordbookId);
        return ResponseEntity.ok(comments);
    }
}
