package wordbook.backend.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import wordbook.backend.api.service.ApiService;

import wordbook.backend.domain.user.dto.UserCreateDTO;
import wordbook.backend.domain.user.service.UserService;

@RestController
@RequestMapping("/user")
public class UserController {
    private UserService userService;
    private final ApiService apiService;
    public UserController(UserService userService, ApiService apiService) {
        this.userService = userService;
        this.apiService = apiService;
    }
    @PostMapping("")
    public ResponseEntity<Long> create(@RequestBody UserCreateDTO userCreateDTO) {
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