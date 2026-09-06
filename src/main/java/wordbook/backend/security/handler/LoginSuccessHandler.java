package wordbook.backend.security.handler;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import wordbook.backend.domain.refresh.service.JWTService;
import wordbook.backend.security.util.JWTUtil;

import java.io.IOException;
@Qualifier("LoginSuccessHandler")
@Component
public class LoginSuccessHandler implements AuthenticationSuccessHandler {
    private final JWTService jwtService;
    private final JWTUtil jwtUtil;
    public LoginSuccessHandler(JWTUtil jwtUtil,JWTService jwtService) {
       this.jwtService = jwtService;
        this.jwtUtil = jwtUtil;
    }
    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {
        String username = authentication.getName();
        String role = authentication.getAuthorities().iterator().next().getAuthority();
        String accessToken= jwtUtil.createToken(username,role,true);
        String refreshToken=jwtUtil.createToken(username,role,false);

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        jwtService.addRefresh(username, refreshToken);
        String json = String.format("{\"accessToken\":\"%s\"}", accessToken);
        response.addCookie(createCookie("refresh",refreshToken));
        response.getWriter().write(json);
        response.getWriter().flush();
    }
    private Cookie createCookie(String key, String value){
        Cookie cookie=new Cookie(key,value);
        cookie.setMaxAge(60*60*24);
        cookie.setHttpOnly(true);
        cookie.setPath("/");
        cookie.setSecure(true);
        return cookie;
    }
}
