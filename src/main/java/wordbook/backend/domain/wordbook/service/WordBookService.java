package wordbook.backend.domain.wordbook.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import wordbook.backend.domain.user.entity.UserEntity;
import wordbook.backend.domain.user.service.UserService;
import wordbook.backend.domain.wordbook.dto.WordBookRequestDTO;
import wordbook.backend.domain.wordbook.dto.WordBookListResponseDTO;
import wordbook.backend.domain.wordbook.entity.WordBookEntity;
import wordbook.backend.domain.wordbook.repository.WordBookRepository;

import java.util.List;

@Service
public class WordBookService {
    private final UserService userService;
    private final WordBookRepository wordBookRepository;

    public WordBookService( WordBookRepository wordBookRepository, UserService userService) {
        this.userService = userService;
        this.wordBookRepository = wordBookRepository;

    }
    @Transactional(readOnly = true)
    public List<WordBookListResponseDTO> findMyAll(String username) {
        UserEntity user= userService.findUserByUsername(username);
        return wordBookRepository.findWordBooksWithWordCount(user);

    }
    @Transactional
    public WordBookListResponseDTO save(WordBookRequestDTO wordBookRequestDTO, String username) {
        UserEntity user = userService.findUserByUsername(username);
        WordBookEntity wordBook= WordBookEntity.builder()
                .userEntity(user)
                .name(wordBookRequestDTO.getName())
                .build();
        WordBookEntity save = wordBookRepository.save(wordBook);
        return WordBookListResponseDTO.builder()
                .id(save.getId())
                .name(wordBookRequestDTO.getName())
                .build();
    }
    //단어장 삭제
    @Transactional
    public void remove(Long id, String username) {
        Long userId = userService.findUserByUsername(username).getId();
        WordBookEntity wordBook=wordBookRepository.findByIdAndUserEntity_Id(id,userId).orElseThrow(()->new RuntimeException());
        wordBookRepository.delete(wordBook);
    }

}
