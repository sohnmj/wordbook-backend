package wordbook.backend.domain.wordbookword.service;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.ActiveProfiles;
import wordbook.backend.domain.user.service.UserService;
import wordbook.backend.domain.word.dto.WordResponseDTO;
import wordbook.backend.domain.word.entity.WordEntity;
import wordbook.backend.domain.word.repository.WordRepository;
import wordbook.backend.domain.wordbook.repository.WordBookRepository;
import wordbook.backend.domain.wordbookword.repository.WordBookWordRepository;

import java.util.List;

import static org.awaitility.Awaitility.given;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
//@ActiveProfiles("test")
class WordBookWordServiceTest extends WordBookWord{
    @Mock
    private WordBookWordRepository wordBookWordRepository;
    @Mock
    private  WordRepository wordRepository;
    @Mock
    private  WordBookRepository wordBookRepository;
    @Mock
    private  UserService userService;
    @InjectMocks
    private WordBookWordService wordBookWordService;
    private static List<WordEntity> testWords;
    @BeforeAll
    static void testcase(){
         testWords = List.of(
                WordEntity.builder()
                        .id(1L)
                        .word("apple")
                        .meaning("사과")
                        .lang("EN")
                        .example("I eat an apple every morning.")
                        .translation("나는 매일 아침 사과를 먹는다.")
                        .useword("apple")
                        .exampleStartIndex(10)
                        .exampleLastIndex(14)
                        .topic("fruit")
                        .build(),

                WordEntity.builder()
                        .id(2L)
                        .word("library")
                        .meaning("도서관")
                        .lang("EN")
                        .example("She studies in the library after school.")
                        .translation("그녀는 방과 후 도서관에서 공부한다.")
                        .useword("library")
                        .exampleStartIndex(21)
                        .exampleLastIndex(27)
                        .topic("place")
                        .build(),

                WordEntity.builder()
                        .id(3L)
                        .word("computer")
                        .meaning("컴퓨터")
                        .lang("EN")
                        .example("This computer is very fast.")
                        .translation("이 컴퓨터는 매우 빠르다.")
                        .useword("computer")
                        .exampleStartIndex(5)
                        .exampleLastIndex(12)
                        .topic("technology")
                        .build(),

                WordEntity.builder()
                        .id(4L)
                        .word("travel")
                        .meaning("여행하다")
                        .lang("EN")
                        .example("I want to travel around the world.")
                        .translation("나는 세계 여행을 하고 싶다.")
                        .useword("travel")
                        .exampleStartIndex(10)
                        .exampleLastIndex(15)
                        .topic("verb")
                        .build(),

                WordEntity.builder()
                        .id(5L)
                        .word("beautiful")
                        .meaning("아름다운")
                        .lang("EN")
                        .example("The sunset is beautiful today.")
                        .translation("오늘 석양은 아름답다.")
                        .useword("beautiful")
                        .exampleStartIndex(15)
                        .exampleLastIndex(23)
                        .topic("adjective")
                        .build()
        );
    }

    @Test
    public void tesetwords(){
        //given
        int size=10;
        int wordbookId=1;
        //when
        when(wordRepository.findTestWord(anyLong(), any(Pageable.class)))
                .thenReturn(testWords);
        //then
        List<WordResponseDTO> testWords1 = wordBookWordService.getTestWords(size, wordbookId);
        for(WordResponseDTO w: testWords1){
            System.out.println("w.getWord() = " + w.getWord());
        }
    }

}