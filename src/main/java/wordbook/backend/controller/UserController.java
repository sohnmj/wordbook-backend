package wordbook.backend.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import wordbook.backend.api.service.ApiService;

import wordbook.backend.domain.user.dto.UserCreateDTO;
import wordbook.backend.domain.user.service.UserService;
import wordbook.backend.mail.MailService;
import wordbook.backend.redis.RedisService;
@Slf4j
@RestController
@RequestMapping("/api/v2/user")
public class UserController {
    private UserService userService;
    private final ApiService apiService;
    private final RedisService redisService;
    public UserController(UserService userService, ApiService apiService, RedisService redisService) {
        this.userService = userService;
        this.apiService = apiService;
        this.redisService = redisService;
    }
    @PostMapping("/")
    public ResponseEntity<Long> create(@RequestBody UserCreateDTO userCreateDTO) {
        //검증된 이메일인지 검사
        String email=userCreateDTO.getEmail();

        boolean isVerifiedEmail = redisService.IsVerifiedEmail("email:" + email);
        log.info("userCreateDTO:{}",isVerifiedEmail);
        if(!isVerifiedEmail){
            throw new RuntimeException();
        }
        Long id=userService.joinUser(userCreateDTO);
        return ResponseEntity.ok(id);
    }
    @PostMapping("/exist")
    public ResponseEntity<Boolean>existUser(@RequestBody UserCreateDTO userCreateDTO) {
        String username=userCreateDTO.getUsername();
        Boolean exited = userService.existUser(username);
        return ResponseEntity.ok(exited);
    }
    @GetMapping("/remove")
    public ResponseEntity<String>removeUser(Authentication authentication) {
        String username=authentication.getName();
        userService.removeUserByUsername(username);
        return ResponseEntity.ok(username);
    }
}