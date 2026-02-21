package wordbook.backend.domain.wordbookword.repository;


import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import wordbook.backend.domain.word.entity.WordEntity;
import wordbook.backend.domain.wordbookword.entity.WordBookWordEntity;

import java.util.List;
import java.util.Optional;

@Repository
public interface WordBookWordRepository extends JpaRepository<WordBookWordEntity,Long> {
    @Query("""
    select wbw
    from WordBookWordEntity wbw
    join fetch wbw.wordEntity
    where wbw.wordBookEntity.id = :wordBookId
""")
    List<WordBookWordEntity> findWithWord(Long wordBookId);


    Optional<WordBookWordEntity> findByWordBookEntity_IdAndWordEntity_Id(Long wordBookEntityId, Long wordEntityId);
}
