package wordbook.backend.domain.user.service;


import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import wordbook.backend.domain.user.dto.UserCreateDTO;
import wordbook.backend.domain.user.entity.UserEntity;
import wordbook.backend.domain.user.repository.UserRepository;

@Service
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
                .IsSocial(false)
                .usage(0)
                .build();
        return userRepository.save(userEntity).getId();
    }
    @Transactional
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
    //usage업데이트
    @Transactional
    public void updateUsage(UserEntity userEntity) {
        userEntity.updateUsage(10);
        userRepository.save(userEntity);
    }
    //usage 0이상일때 true
    @Transactional(readOnly = true)
    public boolean possibleUsage(String username) {
        UserEntity user = findUserByUsername(username);
        int usage = user.getUsage();
        if(usage <= 0){
            return false;
        }
        return true;
    }
}
