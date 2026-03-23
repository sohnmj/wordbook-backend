package wordbook.backend;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import wordbook.backend.domain.user.dto.UserCreateDTO;
import wordbook.backend.domain.user.entity.UserEntity;
import wordbook.backend.domain.user.repository.UserRepository;
import wordbook.backend.domain.user.service.UserService;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {
    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Test
    public void createUser() {
        //given
        UserCreateDTO userCreateDTO = new UserCreateDTO("admin","email","1234");
        when(passwordEncoder.encode("1234")).thenReturn("5678");
        when(userRepository.save(any(UserEntity.class)))
                .thenAnswer(invocation -> {
                    UserEntity user = invocation.getArgument(0);
                    user.setId(1L);
                    return user;
                });
        UserService userService = new UserService(userRepository, passwordEncoder);
        Long testid=userService.joinUser(userCreateDTO);
        then(userRepository).should().save(any());
        assertThat(testid).isEqualTo(1L);
    }

}