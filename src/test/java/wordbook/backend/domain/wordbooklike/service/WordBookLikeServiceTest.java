package wordbook.backend.domain.wordbooklike.service;


import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.dao.DataIntegrityViolationException;
import wordbook.backend.domain.user.entity.UserEntity;
import wordbook.backend.domain.user.service.UserService;
import wordbook.backend.domain.wordbook.entity.WordBookEntity;
import wordbook.backend.domain.wordbook.service.WordBookService;
import wordbook.backend.domain.wordbooklike.entiy.WordBookLikeEntity;
import wordbook.backend.domain.wordbooklike.repository.WordBookLikeRepository;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.verify;


@ExtendWith(MockitoExtension.class)
class WordBookLikeServiceTest{
    @Mock
    public UserService userService;
    @Mock
    public WordBookService wordBookService;
    @Mock
    public WordBookLikeRepository wordBookLikeRepository;
    @InjectMocks
    public WordBookLikeService wordBookLikeService;

    @Test
    void 좋아요_추가_성공() {
        // given
        long wordId = 1L;
        String username = "testUser";

        WordBookEntity wordBook = WordBookEntity.builder().build();
        UserEntity user = UserEntity.builder().build();

        given(wordBookService.getById(wordId))
                .willReturn(wordBook);

        given(userService.findUserByUsername(username))
                .willReturn(user);
        // when
        boolean result =
                wordBookLikeService.alterWordBookLike(wordId, username);

        // then
        assertThat(result).isTrue();

        verify(wordBookLikeRepository)
                .save(any(WordBookLikeEntity.class));
    }

    @Test
    void 이미_좋아요가_존재하면_삭제() {

        // given
        long wordId = 1L;
        String username = "testUser";

        WordBookEntity wordBook = WordBookEntity.builder().build();
        UserEntity user = UserEntity.builder().build();

        given(wordBookService.getById(wordId))
                .willReturn(wordBook);

        given(userService.findUserByUsername(username))
                .willReturn(user);
        willThrow(DataIntegrityViolationException.class)
                .given(wordBookLikeRepository)
                .save(any(WordBookLikeEntity.class));

        // when
        boolean result =
                wordBookLikeService.alterWordBookLike(wordId, username);

        // then
        assertThat(result).isFalse();

        verify(wordBookLikeRepository)
                .deleteWordBookLikeEntityByWordBookEntityAndUserEntity(
                        wordBook,
                        user
                );
    }
}