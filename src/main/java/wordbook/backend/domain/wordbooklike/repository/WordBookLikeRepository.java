package wordbook.backend.domain.wordbooklike.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import wordbook.backend.domain.user.entity.UserEntity;
import wordbook.backend.domain.wordbook.entity.WordBookEntity;
import wordbook.backend.domain.wordbooklike.entiy.WordBookLikeEntity;

import java.util.List;

@Repository
public interface WordBookLikeRepository extends JpaRepository<WordBookLikeEntity,Long> {
   List<WordBookLikeEntity> findWordBookLikeEntitiesByWordBookEntity(WordBookEntity wordBookEntity);
   void deleteWordBookLikeEntityByWordBookEntityAndUserEntity(WordBookEntity wordBook, UserEntity user);
   Boolean existsByWordBookEntityAndUserEntity(WordBookEntity wordBook, UserEntity user);
}
