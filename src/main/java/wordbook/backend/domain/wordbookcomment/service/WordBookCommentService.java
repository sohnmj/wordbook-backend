package wordbook.backend.domain.wordbookcomment.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestParam;
import wordbook.backend.domain.user.entity.UserEntity;
import wordbook.backend.domain.user.service.UserService;
import wordbook.backend.domain.wordbook.entity.WordBookEntity;
import wordbook.backend.domain.wordbook.service.WordBookService;
import wordbook.backend.domain.wordbookcomment.dto.WordBookCommentRequestDTO;
import wordbook.backend.domain.wordbookcomment.dto.WordBookCommentResponseDTO;
import wordbook.backend.domain.wordbookcomment.entity.WordBookCommentEntity;
import wordbook.backend.domain.wordbookcomment.repository.WordBookCommentRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
public class WordBookCommentService {

    private final WordBookCommentRepository wordBookCommentRepository;
    private final WordBookService wordBookService;
    private final UserService userService;

    //comment 생성
    public Long createComment(WordBookCommentRequestDTO wordBookCommentRequestDTO) {
        UserEntity userEntity= userService.findUserByUsername(wordBookCommentRequestDTO.getUsername());
        WordBookEntity wordBookEntity=wordBookService.getById(wordBookCommentRequestDTO.getWordBookId());
        if(wordBookCommentRequestDTO.getParentId() == null){
            WordBookCommentEntity wordBookCommentEntity= WordBookCommentEntity.builder()
                    .wordBookEntity(wordBookEntity)
                    .content(wordBookCommentRequestDTO.getComment())
                    .userEntity(userEntity)
                    .build();
            WordBookCommentEntity save = wordBookCommentRepository.save(wordBookCommentEntity);
            return save.getId();

        }
        else{
            WordBookCommentEntity parentCommentEntity= wordBookCommentRepository.findById(wordBookCommentRequestDTO.getParentId()).orElseThrow(()->new RuntimeException());
            WordBookCommentEntity wordBookCommentEntity= WordBookCommentEntity.builder()
                    .wordBookEntity(wordBookEntity)
                    .content(wordBookCommentRequestDTO.getComment())
                    .userEntity(userEntity)
                    .parentEntity(parentCommentEntity)
                    .build();
            WordBookCommentEntity save = wordBookCommentRepository.save(wordBookCommentEntity);
            return save.getId();
        }
    }
    public List<WordBookCommentResponseDTO> getComments(Long wordbookId) {
        WordBookEntity wordBook = wordBookService.getById(wordbookId);
        List<WordBookCommentResponseDTO> commendList = wordBookCommentRepository.findWordBookCommentEntitiesByWordBookEntityAndParentEntity(wordBook, null);
        return commendList;
    }
}
