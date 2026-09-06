package wordbook.backend.domain.wordbook.service;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PostAuthorize;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import wordbook.backend.domain.user.entity.UserEntity;
import wordbook.backend.domain.user.service.UserService;
import wordbook.backend.domain.wordbook.dto.WordBookRequestDTO;
import wordbook.backend.domain.wordbook.dto.WordBookListResponseDTO;
import wordbook.backend.domain.wordbook.entity.WordBookEntity;
import wordbook.backend.domain.wordbook.repository.WordBookRepository;
import wordbook.backend.redis.RedisService;

import java.util.List;

@Service
public class WordBookService {
    private final UserService userService;
    private final WordBookRepository wordBookRepository;
    private final RedisService redisService;
    public WordBookService( WordBookRepository wordBookRepository, UserService userService, RedisService redisService) {
        this.userService = userService;
        this.wordBookRepository = wordBookRepository;
        this.redisService=redisService;
    }
    @Transactional(readOnly = true)
    public List<WordBookListResponseDTO> findMyAll(String username) {
        UserEntity user= userService.findUserByUsername(username);
        return wordBookRepository.findAllByUserEntity(user).stream().map(wordbook->
                WordBookListResponseDTO.builder().
                        id(wordbook.getId()).
                        name(wordbook.getName()).
                        likeCount(wordbook.getLikeCount()).
                        count(wordbook.getWordCount()).
                        build()).toList();

    }
    @Transactional
    public WordBookListResponseDTO save(WordBookRequestDTO wordBookRequestDTO, String username) {
        UserEntity user = userService.findUserByUsername(username);
        WordBookEntity wordBook= WordBookEntity.builder()
                .userEntity(user)
                .name(wordBookRequestDTO.getName())
                .likeCount(0L)
                .build();
        WordBookEntity save = wordBookRepository.save(wordBook);
        return WordBookListResponseDTO.builder()
                .id(save.getId())
                .name(wordBookRequestDTO.getName())
                .likeCount(save.getLikeCount())
                .build();
    }
    //단어장 삭제
    @Transactional
    public void remove(Long id, String username) {
        Long userId = userService.findUserByUsername(username).getId();
        WordBookEntity wordBook=wordBookRepository.findByIdAndUserEntity_Id(id,userId).orElseThrow(()->new RuntimeException());
        wordBookRepository.delete(wordBook);
    }
    //단어장 찾기
    public WordBookEntity getById(Long id) {
        return wordBookRepository.findById(id).orElseThrow(()->new RuntimeException());
    }

    //시간순/좋아요순 단어장 리스트 반환
    @Transactional(readOnly = true)
    public List<WordBookListResponseDTO> getWordbookList(Pageable pageable){
            List<WordBookListResponseDTO> list = wordBookRepository.findAll(pageable).getContent().stream()
                    .map(wordbook -> WordBookListResponseDTO.builder()
                            .count(wordbook.getWordCount())
                            .id(wordbook.getId())
                            .likeCount(wordbook.getLikeCount())
                            .name(wordbook.getName())
                            .build()
                    ).toList();
            return list;


    }

}
