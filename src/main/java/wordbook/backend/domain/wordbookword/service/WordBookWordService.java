package wordbook.backend.domain.wordbookword.service;

import jakarta.transaction.Transactional;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
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
    public WordBookResponseDTO getWordBook(long id){
        String wordName=wordBookRepository.findById(id).orElseThrow(()->new RuntimeException()).getName();
        List<WordResponseDTO> words = wordBookWordRepository.findWithWord(id).stream()
                .map(wbw->WordResponseDTO.builder()
                        .wordId(wbw.getWordEntity().getId())
                        .word(wbw.getWordEntity().getWord())
                        .lang(wbw.getWordEntity().getLang())
                        .meaning(wbw.getWordEntity().getMeaning())
                        .example(wbw.getWordEntity().getExample())
                        .synonym(wbw.getWordEntity().getSynonym())
                        .antonym(wbw.getWordEntity().getAntonym())
                        .build())
                .toList();
        return new WordBookResponseDTO(words,wordName);
    }

    //단어장에 단어 추가하기
    public Long createWordBookWord(Long wordId,Long wordbookId){
        WordBookEntity wordBookEntity = wordBookRepository.findById(wordbookId).orElseThrow(()-> new RuntimeException("not found"));
        WordEntity wordEntity = wordRepository.findById(wordId).orElseThrow(()-> new RuntimeException("not found"));
        if (!wordEntity.getUserEntity().getId()
                .equals(wordBookEntity.getUserEntity().getId())) {
            throw new RuntimeException("not found");
        }
        WordBookWordEntity wordBookWordEntity = WordBookWordEntity.builder()
                .wordBookEntity(wordBookEntity)
                .wordEntity(wordEntity)
                .build();
        return wordBookWordRepository.save(wordBookWordEntity).getId();

    }
    //시험 볼 수 있는 단어 보여주기
    public List<WordResponseDTO> getTestWords(long id,int size){
        return wordRepository.findTestWord(id, PageRequest.of(0,size)).stream()
                .map(word->WordResponseDTO.builder()
                        .word(word.getWord())
                        .lang(word.getLang())
                        .meaning(word.getMeaning())
                        .example(word.getExample())
                        .synonym(word.getSynonym())
                        .antonym(word.getAntonym())
                        .build()
                        ).toList();

    }
    //단어장에서 단어 보여주기
    @Transactional
    public long removeWordBookWord(long wordBookId,long wordId,String username){
        UserEntity user = userService.findUserByUsername(username);
        WordBookEntity wordBook=wordBookRepository.findById(wordBookId).orElseThrow(()->new RuntimeException());
        if(!user.getId().equals(wordBook.getUserEntity().getId())) {
            throw new RuntimeException();
        }
        WordBookWordEntity wordBookWordEntity = wordBookWordRepository.findByWordBookEntity_IdAndWordEntity_Id(wordBookId, wordId).orElseThrow(() -> new RuntimeException("not found"));
        Long id = wordBookWordEntity.getId();
        wordBookWordRepository.delete(wordBookWordEntity);
        return id;
    }
}
