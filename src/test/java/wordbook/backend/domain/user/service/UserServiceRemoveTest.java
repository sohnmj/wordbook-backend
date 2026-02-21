package wordbook.backend.domain.user.service;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
class UserServiceRemoveTest {
    @Autowired
    UserService userService;
    @Test

    @Transactional
    void removeUser() {
        String username = "admin1";
        userService.removeUserByUsername(username);
    }
}