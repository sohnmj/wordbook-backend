package wordbook.backend.domain.word.service;

import jakarta.transaction.Transactional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import wordbook.backend.api.ApiResponseDTO;
import wordbook.backend.domain.user.entity.UserEntity;
import wordbook.backend.domain.user.repository.UserRepository;
import wordbook.backend.domain.word.dto.WordResponseDTO;
import wordbook.backend.domain.word.entity.WordEntity;
import wordbook.backend.domain.word.repository.WordRepository;


import java.util.Optional;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;


@SpringBootTest
class WordServiceTest {
    @Autowired
    private WordService wordService;
    @Test
    @Transactional
    void createWord() {
        //given


        WordResponseDTO wordResponseDTO=WordResponseDTO.builder()
                .word("test")
                .lang("test")
                .antonym("test")
                .synonym("test")
                .meaning("test")
                .example("test")
                .build();
        String username = "admin1";


        //then
        WordResponseDTO word1 = wordService.createWord(wordResponseDTO,username);
        assertThat(word1.getWord()).isEqualTo(wordResponseDTO.getWord());
    }
}