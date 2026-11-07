package wordbook.backend.domain.user.service;


import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import wordbook.backend.domain.user.dto.UserCreateDTO;
import wordbook.backend.domain.user.entity.UserEntity;
import wordbook.backend.domain.user.entity.UserRoleType;
import wordbook.backend.domain.user.repository.UserRepository;

@Service
@Slf4j
public class UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }
    @Transactional
    public Long joinUser(UserCreateDTO userCreateDTO) {
        UserEntity userEntity= UserEntity.builder()
                .username(userCreateDTO.getUsername())
                .password(passwordEncoder.encode(userCreateDTO.getPassword()))
                .email(userCreateDTO.getEmail())
                .isSocial(false)
                .role(UserRoleType.USER)
                .usage(0)
                .build();
        return userRepository.save(userEntity).getId();
    }
    @Transactional(readOnly = true)
    public UserEntity findUserByUsername(String username) {
        return userRepository.findByUsername(username).orElseThrow(()->new UsernameNotFoundException("user not found"));
    }
    @Transactional(readOnly = true)
    public Boolean existUser(String username) {
        return  userRepository.findByUsername(username).isPresent();
    }
    @Transactional
    public Long removeUserByUsername(String username) {
        UserEntity user = userRepository.findByUsername(username).orElseThrow(() -> new UsernameNotFoundException("user not found"));
        Long userId = user.getId();
        userRepository.delete(user);
        return userId;
    }
    //일일 usage 지급
    @Transactional
    public void grantDailyUsage(String username) {
        findUserByUsername(username).updateDailyUsage();
    }
    //usage 1회 차감 (조건부 UPDATE로 원자적 처리, 잔량이 없으면 예외)
    @Transactional
    public void consumeUsage(String username) {
        if (userRepository.consumeUsage(username) == 0) {
            throw new IllegalStateException("일일 이용 횟수를 모두 소진하였습니다.");
        }
    }
    //usage 0초과일때 true
    @Transactional(readOnly = true)
    public boolean possibleUsage(String username) {
        return findUserByUsername(username).canUse();
    }
}
