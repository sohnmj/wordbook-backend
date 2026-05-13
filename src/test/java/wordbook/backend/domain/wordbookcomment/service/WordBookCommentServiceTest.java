package wordbook.backend.domain.wordbookcomment.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import wordbook.backend.domain.user.entity.UserEntity;
import wordbook.backend.domain.user.service.UserService;
import wordbook.backend.domain.wordbook.entity.WordBookEntity;
import wordbook.backend.domain.wordbook.service.WordBookService;
import wordbook.backend.domain.wordbookcomment.dto.WordBookCommentRequestDTO;
import wordbook.backend.domain.wordbookcomment.entity.WordBookCommentEntity;
import wordbook.backend.domain.wordbookcomment.repository.WordBookCommentRepository;

import java.util.Optional;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WordBookCommentServiceTest {
    @Mock
    private WordBookCommentRepository wordBookCommentRepository;
    @Mock
    private UserService userService;
    @Mock
    private WordBookService wordBookService;
    @InjectMocks
    private WordBookCommentService wordBookCommentService;

    @Test
    void createWordBookComment() {
        WordBookCommentRequestDTO wordBookCommentRequestDTO = new WordBookCommentRequestDTO();
        wordBookCommentRequestDTO.setWordBookId(1L);
        wordBookCommentRequestDTO.setComment("comment");
        wordBookCommentRequestDTO.setParentId(2L);
        wordBookCommentRequestDTO.setUsername("username");
        WordBookCommentEntity wordBookCommentEntity = WordBookCommentEntity.builder()
                .id(1L)
                .build();
        when(userService.findUserByUsername("username")).thenReturn(new UserEntity());
        when(wordBookService.getById(1L)).thenReturn(new WordBookEntity());
        when(wordBookCommentRepository.findById(2L)).thenReturn(Optional.of(new WordBookCommentEntity()));
        when(wordBookCommentRepository.save(any())).thenReturn(wordBookCommentEntity);

        Long comment = wordBookCommentService.createComment(wordBookCommentRequestDTO);
        assertThat(comment).isEqualTo(1L);
    }
}