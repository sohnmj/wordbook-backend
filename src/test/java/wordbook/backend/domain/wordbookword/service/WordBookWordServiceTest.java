package wordbook.backend.domain.wordbookword.service;

import jakarta.transaction.Transactional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import wordbook.backend.domain.word.dto.WordResponseDTO;
import wordbook.backend.domain.wordbook.dto.WordBookResponseDTO;
import wordbook.backend.domain.wordbookword.entity.WordBookWordEntity;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
@SpringBootTest
class WordBookWordServiceTest {
    @Autowired
    private WordBookWordService wordBookWordService;
    @Test
    void getWordBookWord() {
        //given
        Long id = 8L;

        WordBookResponseDTO wordBook = wordBookWordService.getWordBook(id);
        for(WordResponseDTO wordResponseDTO : wordBook.getWords()) {
            System.out.println(wordResponseDTO);
        }
        System.out.println(wordBook.getName());
    }

    @Test
    void getTestWord() {
        Long id = 8L;
        int size=2;
        List<WordResponseDTO> testWords = wordBookWordService.getTestWords(id, size);
        for(WordResponseDTO wordResponseDTO : testWords) {
            System.out.println(wordResponseDTO);
        }
    }
    @Test
    @Transactional
    void remove(){
        //given
        Long wordId=5l;
        Long wordBookId=8l;
        String username="admin";
        wordBookWordService.removeWordBookWord(wordBookId,wordId,username);
    }
}