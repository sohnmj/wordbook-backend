package wordbook.backend.domain.word.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import wordbook.backend.domain.user.entity.UserEntity;
import wordbook.backend.domain.word.entity.WordEntity;
import wordbook.backend.domain.wordbook.entity.WordBookEntity;

import java.util.List;
import java.util.Optional;

@Repository
public interface WordRepository extends JpaRepository<WordEntity,Long> {
    List<WordEntity> findByUserEntity(UserEntity userEntity);
    @Query("""
SELECT w
FROM WordBookWordEntity wbw
JOIN wbw.wordEntity w
WHERE wbw.wordBookEntity.id = :id
ORDER BY function('RAND')
""")
    List<WordEntity> findTestWord(Long id, Pageable pageable);

    Optional<WordEntity> findByIdAndUserEntity_Id(Long id, Long userId);
}

