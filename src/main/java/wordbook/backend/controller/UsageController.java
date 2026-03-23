package wordbook.backend.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import wordbook.backend.domain.user.dto.UserCreateDTO;
import wordbook.backend.domain.user.service.UserService;
import wordbook.backend.usage.UsageService;

@Slf4j
@RestController
@RequestMapping("/api/v2/usage")
public class UsageController {
    private UsageService usageService;
    @Autowired
    public void setUserService(UsageService usageService) {
        this.usageService = usageService;
    }
    @PostMapping("/")
    public ResponseEntity<Boolean> getUsage(Authentication authentication){
        String username=authentication.getName();
        boolean usage = usageService.getUsage(username);
        return ResponseEntity.ok(usage);
    }
}
