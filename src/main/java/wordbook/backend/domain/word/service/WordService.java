package wordbook.backend.domain.word.service;

import org.springframework.stereotype.Service;
import wordbook.backend.domain.user.entity.UserEntity;
import wordbook.backend.domain.user.service.UserService;
import wordbook.backend.domain.word.dto.WordResponseDTO;
import wordbook.backend.domain.word.entity.WordEntity;
import wordbook.backend.domain.word.repository.WordRepository;

import java.util.List;

@Service
public class WordService {
    private final WordRepository wordRepository;
    private final UserService userService;
    public WordService(WordRepository wordRepository, UserService userService) {
        this.wordRepository = wordRepository;
        this.userService = userService;
    }
    public WordResponseDTO createWord( WordResponseDTO wordResponseDTO, String username) {
        UserEntity user = userService.findUserByUsername(username);
        WordEntity wordEntity = WordEntity.builder()
                .userEntity(user)
                .antonym(wordResponseDTO.getAntonym())
                .example(wordResponseDTO.getExample())
                .meaning(wordResponseDTO.getMeaning())
                .synonym(wordResponseDTO.getSynonym())
                .lang(wordResponseDTO.getLang())
                .word(wordResponseDTO.getWord())
                .build();
        WordEntity save = wordRepository.save(wordEntity);
        WordResponseDTO responseDTO= WordResponseDTO.builder()
                .wordId(save.getId())
                .word(save.getWord())
                .antonym(save.getAntonym())
                .example(save.getExample())
                .meaning(save.getMeaning())
                .synonym(save.getSynonym())
                .lang(save.getLang())
                .build();
        return responseDTO;
    }
    public List<WordResponseDTO> getWordList(String username) {
        UserEntity user = userService.findUserByUsername(username);
        return wordRepository.findByUserEntity(user).stream()
                .map(word -> WordResponseDTO.builder()
                        .wordId(word.getId())
                        .word(word.getWord())
                        .antonym(word.getAntonym())
                        .example(word.getExample())
                        .meaning(word.getMeaning())
                        .synonym(word.getSynonym())
                        .lang(word.getLang())
                        .build()).toList();
    }
}
