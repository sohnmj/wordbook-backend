package wordbook.backend.domain.wordbookword.service;


import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import wordbook.backend.domain.user.entity.UserEntity;
import wordbook.backend.domain.user.service.UserService;
import wordbook.backend.domain.word.dto.WordResponseDTO;
import wordbook.backend.domain.word.entity.WordEntity;
import wordbook.backend.domain.word.repository.WordRepository;
import wordbook.backend.domain.wordbook.dto.WordBookResponseDTO;
import wordbook.backend.domain.wordbook.entity.WordBookEntity;
import wordbook.backend.domain.wordbook.repository.WordBookRepository;
import wordbook.backend.domain.wordbookword.entity.WordBookWordEntity;
import wordbook.backend.domain.wordbookword.repository.WordBookWordRepository;

import java.util.List;

@Service
public class WordBookWordService {
    private final WordBookWordRepository  wordBookWordRepository;
    private final WordRepository wordRepository;
    private final WordBookRepository wordBookRepository;
    private final UserService userService;
    public WordBookWordService(WordBookWordRepository wordBookWordRepository, WordRepository wordRepository, WordBookRepository wordBookRepository,UserService userService) {
        this.wordBookWordRepository = wordBookWordRepository;
        this.wordRepository = wordRepository;
        this.wordBookRepository = wordBookRepository;
        this.userService = userService;
    }

    //단어장 보여주기
    @Transactional(readOnly = true)
    public WordBookResponseDTO getWordBook(long id){
        WordBookEntity wordBookEntity = wordBookRepository.findById(id).orElseThrow(() -> new RuntimeException());
        String wordName=wordBookEntity.getName();
        Long likeCount=wordBookEntity.getLikeCount();
        List<WordResponseDTO> words = wordBookWordRepository.findWithWord(id).stream()
                .map(wbw->WordResponseDTO.builder()
                        .wordId(wbw.getWordEntity().getId())
                        .word(wbw.getWordEntity().getWord())
                        .lang(wbw.getWordEntity().getLang())
                        .meaning(wbw.getWordEntity().getMeaning())
                        .example(wbw.getWordEntity().getExample())
                        .topic(wbw.getWordEntity().getTopic())
                        .translation(wbw.getWordEntity().getTranslation())
                        .build())
                .toList();
        return new WordBookResponseDTO(words,wordName,likeCount);
    }

    //단어장에 단어 추가하기(유저에 소속된 건지 확인)
    @Transactional
    public Long createWordBookWord(Long wordId,Long wordbookId,String username){
        Long userId = userService.findUserByUsername(username).getId();
        // 유저 소속 단어와 단어장인지 확인
        WordBookEntity wordBookEntity = wordBookRepository.findByIdAndUserEntity_Id(wordbookId,userId).orElseThrow(()-> new RuntimeException("not found"));
        WordEntity wordEntity = wordRepository.findByIdAndUserEntity_Id(wordId,userId).orElseThrow(()-> new RuntimeException("not found"));
        wordBookEntity.incrementWordCount();
        //저장
        WordBookWordEntity wordBookWordEntity = WordBookWordEntity.builder()
                .wordBookEntity(wordBookEntity)
                .wordEntity(wordEntity)
                .build();
        return wordBookWordRepository.save(wordBookWordEntity).getId();

    }
    //시험 볼 수 있는 단어 보여주기
    @Transactional(readOnly = true)
    public List<WordResponseDTO> getTestWords(long wordbookId,int size){
        return wordRepository.findTestWord(wordbookId, PageRequest.of(0,size)).stream()
                .map(word->WordResponseDTO.builder()
                        .word(word.getWord())
                        .lang(word.getLang())
                        .meaning(word.getMeaning())
                        .example(word.getExample())
                                .translation(word.getTranslation())
                                .useword(word.getUseword())
                                .startIndex(word.getExampleStartIndex())
                                .lastIndex(word.getExampleLastIndex())
                        .build()
                        ).toList();

    }
    //단어장에서 단어 삭제
    @Transactional
    public long removeWordBookWord(long wordBookId,long wordId,String username){
        Long userId = userService.findUserByUsername(username).getId();
        // 유저 소속 단어와 단어장인지 확인
        WordBookEntity wordBookEntity = wordBookRepository.findByIdAndUserEntity_Id(wordBookId,userId).orElseThrow(()-> new RuntimeException("not found"));
        WordEntity wordEntity = wordRepository.findByIdAndUserEntity_Id(wordId,userId).orElseThrow(()-> new RuntimeException("not found"));
        wordBookEntity.decrementWordCount();
        WordBookWordEntity wordBookWordEntity = wordBookWordRepository.findByWordBookEntity_IdAndWordEntity_Id(wordBookId, wordId).orElseThrow(() -> new RuntimeException("not found"));
        Long id = wordBookWordEntity.getId();
        wordBookWordRepository.delete(wordBookWordEntity);
        return id;
    }
}
