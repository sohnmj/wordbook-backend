package wordbook.backend.domain.user.repository;

import wordbook.backend.domain.user.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<UserEntity, Long> {
    Optional<UserEntity> findByUsername (String username);
    Optional<UserEntity> findByUsernameAndIsSocial(String username,Boolean isSocial);

    // 검사(usage > 0)와 차감을 DB에서 한 문장으로 처리해 동시 요청에도 원자적으로 동작한다. 영향받은 행 수를 반환
    @Modifying(clearAutomatically = true)
    @Query("update UserEntity u set u.usage = u.usage - 1 where u.username = :username and u.usage > 0")
    int consumeUsage(@Param("username") String username);
}
