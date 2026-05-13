package wordbook.backend.security.handler;

import jakarta.persistence.Column;
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
@Component
@Qualifier("SocialSuccessHandler")
public class SocialSuccessHandler implements AuthenticationSuccessHandler {
    private final JWTService jwtService;
    private final JWTUtil jwtUtil;
    public SocialSuccessHandler(JWTService jwtService, JWTUtil jwtUtil) {
        this.jwtService = jwtService;
        this.jwtUtil=jwtUtil;
    }
    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {
        String username = authentication.getName();
        String role = authentication.getAuthorities().iterator().next().getAuthority();



        // JWT(Refresh) 발급
        String refreshToken = jwtUtil.createToken(username, "ROLE_" + role, false);

        // 발급한 Refresh DB 테이블 저장 (Refresh whitelist)
        jwtService.addRefresh(username, refreshToken);

        // 응답
        Cookie refreshCookie = new Cookie("refreshToken", refreshToken);
        refreshCookie.setHttpOnly(true);
        refreshCookie.setSecure(false);
        refreshCookie.setPath("/");
        refreshCookie.setMaxAge(100);

        response.addCookie(refreshCookie);
//        response.sendRedirect("http://localhost:5173/cookie");

    }
}
