package wordbook.backend.domain.wordbooklike.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import wordbook.backend.domain.user.entity.UserEntity;
import wordbook.backend.domain.user.service.UserService;
import wordbook.backend.domain.wordbook.entity.WordBookEntity;
import wordbook.backend.domain.wordbook.service.WordBookService;
import wordbook.backend.domain.wordbooklike.entiy.WordBookLikeEntity;
import wordbook.backend.domain.wordbooklike.repository.WordBookLikeRepository;



@Service
@RequiredArgsConstructor
public class WordBookLikeService {
    private final WordBookService wordBookService;
    private final UserService userService;
    private final WordBookLikeRepository wordBookLikeRepository;
    @Transactional
    public boolean alterWordBookLike(long wordId,String username){
        WordBookEntity wordBook = wordBookService.getById(wordId);
        UserEntity user = userService.findUserByUsername(username);
        boolean exists = wordBookLikeRepository.existsByWordBookEntityAndUserEntity(wordBook, user);
        if(!exists) {
            WordBookLikeEntity wordBookLike = WordBookLikeEntity.builder()
                    .wordBookEntity(wordBook)
                    .userEntity(user)
                    .build();
            wordBookLikeRepository.save(wordBookLike);
            wordBook.incrementLikeCount();
            return true;
        }
        else{
           wordBookLikeRepository.deleteWordBookLikeEntityByWordBookEntityAndUserEntity(wordBook,user);
           wordBook.decrementLikeCount();
           return false;
        }
    }

}
