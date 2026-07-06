package wordbook.backend.domain.word.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
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
    //단어 생성
    @Transactional
    public WordResponseDTO createWord( WordResponseDTO wordResponseDTO, String username) {
        UserEntity user = userService.findUserByUsername(username);
        getIndexFromExample(wordResponseDTO);
        WordEntity wordEntity = WordEntity.builder()
                .userEntity(user)
                .example(wordResponseDTO.getExample())
                .meaning(wordResponseDTO.getMeaning())
                .lang(wordResponseDTO.getLang())
                .word(wordResponseDTO.getWord())
                .topic(wordResponseDTO.getTopic())
                .translation(wordResponseDTO.getTranslation())
                .exampleLastIndex(wordResponseDTO.getLastIndex())
                .exampleStartIndex(wordResponseDTO.getStartIndex())
                .useword(wordResponseDTO.getUseword())
                .build();
        WordEntity save = wordRepository.save(wordEntity);
        return WordResponseDTO.builder()
                .wordId(save.getId())
                .word(save.getWord())
                .example(save.getExample())
                .meaning(save.getMeaning())
                .lang(save.getLang())
                .topic(save.getTopic())
                .translation(save.getTranslation())
                .lastIndex(save.getExampleLastIndex())
                .startIndex(save.getExampleStartIndex())
                .useword(save.getUseword())
                .build();
    }

    @Transactional(readOnly = true)
    public List<WordResponseDTO> getWordList(String username) {
        UserEntity user = userService.findUserByUsername(username);
        return wordRepository.findByUserEntity(user).stream()
                .map(word -> WordResponseDTO.builder()
                        .wordId(word.getId())
                        .word(word.getWord())
                        .example(word.getExample())
                        .meaning(word.getMeaning())
                        .topic(word.getTopic())
                        .lang(word.getLang())
                        .build()).toList();
    }

    //예문에서 단어 위치 찾기
    public void getIndexFromExample(WordResponseDTO wordResponseDTO) {
        String example = wordResponseDTO.getExample();
        String word=wordResponseDTO.getUseword();
        int startIndex = example.indexOf(word);
        int lastIndex = startIndex + word.length()-1;
        wordResponseDTO.setStartIndex(startIndex);
        wordResponseDTO.setLastIndex(lastIndex);
    }
}
