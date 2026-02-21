package wordbook.backend.domain.wordbook.service;

import jakarta.transaction.Transactional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Rollback;
import wordbook.backend.domain.wordbook.dto.WordBookRequestDTO;
import wordbook.backend.domain.wordbook.dto.WordBookListResponseDTO;
import wordbook.backend.domain.wordbook.repository.WordBookRepository;
import wordbook.backend.domain.wordbookword.repository.WordBookWordRepository;


import java.util.List;

@SpringBootTest
class WordBookServiceTest {
    @Autowired
    private WordBookService wordBookService;
    @Autowired
    private WordBookRepository wordBookRepository;
    @Autowired
    private WordBookWordRepository wordBookWordRepository;

    @Test

    @Transactional
    public void createWordBook() {
        //given
        String username="admin1";
        String wordBookName="korean1";
        WordBookRequestDTO wordBookRequestDTO=new WordBookRequestDTO(wordBookName);
        //
         wordBookService.save(wordBookRequestDTO, username);

    }
    @Test
    public void getWordBook() {
        //given
        String username="admin1";

        List<WordBookListResponseDTO> myAll = wordBookService.findMyAll(username);
        for (WordBookListResponseDTO wordBookResponseDTO : myAll) {
            System.out.println(wordBookResponseDTO);
        }

    }
    @Transactional
    @Test
    @Rollback(false)
    public void remove() {
//        //given
//        String username="admin1";
//        long id=8L;
//        wordBookService.remove(id,username);
//        boolean present = wordBookRepository.findById(id).isPresent();
//        System.out.println("present = " + present);
//        boolean present1 = wordBookWordRepository.findById(10l).isPresent();
//        System.out.println("present1 = " + present1);
        String username="admin1";
        Long id=10L;
        wordBookService.remove(id, username);
    }




}