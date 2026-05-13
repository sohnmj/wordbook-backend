package wordbook.backend.domain.wordbookcomment.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import wordbook.backend.domain.wordbook.entity.WordBookEntity;
import wordbook.backend.domain.wordbookcomment.dto.WordBookCommentResponseDTO;
import wordbook.backend.domain.wordbookcomment.entity.WordBookCommentEntity;

import java.util.List;

public interface WordBookCommentRepository extends JpaRepository<WordBookCommentEntity,Long> {
    @Query("""
select new wordbook.backend.domain.wordbookcomment.dto.WordBookCommentResponseDTO(
wbc.content,
wbc.userEntity.id,
wbc.userEntity.username

)from WordBookCommentEntity wbc
join UserEntity user
    on wbc.userEntity = user
where wbc.parentEntity = :parent
and wbc.wordBookEntity=:wordBook
            """)
   List<WordBookCommentResponseDTO> findWordBookCommentEntitiesByWordBookEntityAndParentEntity(WordBookEntity wordBook, WordBookCommentEntity parent);
}
