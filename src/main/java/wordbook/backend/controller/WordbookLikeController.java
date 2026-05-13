package wordbook.backend.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import wordbook.backend.domain.wordbooklike.service.WordBookLikeService;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v2/wordbooklike")
public class WordbookLikeController {
    private final WordBookLikeService wordBookLikeService;
    @PostMapping("/{wordbook_id}")
    public ResponseEntity<Boolean> alterLike(Authentication authentication,@PathVariable Long wordbook_id){
        String username=authentication.getName();
        Boolean isAddLike=wordBookLikeService.alterWordBookLike(wordbook_id,username);
        return ResponseEntity.ok().body(isAddLike);
    }
}
