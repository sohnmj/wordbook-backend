package wordbook.backend.domain.wordbook.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import wordbook.backend.domain.user.entity.UserEntity;
import wordbook.backend.domain.wordbook.dto.WordBookListResponseDTO;
import wordbook.backend.domain.wordbook.entity.WordBookEntity;

import java.util.List;


@Repository
public interface WordBookRepository extends JpaRepository<WordBookEntity,Long> {
    @Query("""
select new wordbook.backend.domain.wordbook.dto.WordBookListResponseDTO(
    wb.id,
    wb.name,
    count(wbw.id)
)
from WordBookEntity wb
left join WordBookWordEntity wbw
    on wbw.wordBookEntity = wb
where wb.userEntity = :user
group by wb.id, wb.name
""")
    List<WordBookListResponseDTO> findWordBooksWithWordCount(UserEntity user);
}
