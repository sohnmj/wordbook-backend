package wordbook.backend.domain.wordbook.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import wordbook.backend.domain.user.entity.UserEntity;
import wordbook.backend.domain.wordbook.dto.WordBookListResponseDTO;
import wordbook.backend.domain.wordbook.entity.WordBookEntity;

import java.util.List;
import java.util.Optional;


@Repository
public interface WordBookRepository extends JpaRepository<WordBookEntity,Long> {

    List<WordBookEntity> findAllByUserEntity(UserEntity user);


    Optional<WordBookEntity> findByIdAndUserEntity_Id(Long wordBookId,Long userId);

    Page<WordBookEntity> findAll(Pageable pageable);
}
