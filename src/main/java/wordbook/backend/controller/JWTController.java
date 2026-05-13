package wordbook.backend.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import wordbook.backend.domain.refresh.dto.JWTResponseDTO;
import wordbook.backend.domain.refresh.service.JWTService;
@Slf4j
@RestController
@RequestMapping("/api/v2/jwt")
public class JWTController {
    private final JWTService jwtService;
    public JWTController(JWTService jwtService) {
        this.jwtService = jwtService;
    }
    @PostMapping(value="/exchange", consumes= MediaType.APPLICATION_JSON_VALUE)
    public JWTResponseDTO jwtExchangeApi(HttpServletRequest request, HttpServletResponse response){
        log.info("request : {}",request);
        return jwtService.refreshRotate(request,response);
    }
}
