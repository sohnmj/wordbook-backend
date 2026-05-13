package wordbook.backend.domain.refresh.service;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import wordbook.backend.domain.refresh.dto.JWTResponseDTO;

import wordbook.backend.redis.RedisService;
import wordbook.backend.security.util.JWTUtil;

@Slf4j
@Service
public class JWTService {

    private JWTUtil jwtUtil;
    private RedisService redisService;
    public JWTService( JWTUtil jwtUtil, RedisService redisService) {
        this.jwtUtil = jwtUtil;
        this.redisService = redisService;
    }

    public JWTResponseDTO refreshRotate(HttpServletRequest request, HttpServletResponse response) {
        Cookie[] cookies=request.getCookies();

        //쿠키에 내용 없으면
        if(cookies==null){
            log.warn("cookies is null");
            throw new RuntimeException();
        }
        String refreshToken=null;
        for(Cookie cookie:cookies){
            if(cookie.getName().equals("refresh")){
                refreshToken=cookie.getValue();
            }
        }

        //쿠키에 refresh 토큰 없으면
        if(refreshToken==null){
            log.warn("refreshToken is null");
            throw new RuntimeException();
        }

        Boolean isValid=jwtUtil.isValid(refreshToken,false);
        if(!isValid){
            log.warn("refreshToken is invalid");
            throw new RuntimeException();
        }
        String username=jwtUtil.getUsername(refreshToken);
        String role=jwtUtil.getRole(refreshToken);
        Boolean isExist=redisService.isTrue("user:"+username);
        if(!isExist){
            log.warn("refreshToken isn't exist");
            throw new RuntimeException();
        }
        else {
            if(!refreshToken.equals(redisService.getRedis("user:"+username))){
                log.warn("refreshToken is wrong");
            }
        }
        log.info("username:{} role:{}",username,role);

        //토큰 생성
        String newAccessToken=jwtUtil.createToken(username,role,true);
        String newRefreshToken= jwtUtil.createToken(username,role, false);

        addRefresh(username,newRefreshToken);

        Cookie refreshCookie = new Cookie("refreshToken", newRefreshToken);
        refreshCookie.setHttpOnly(true);
        refreshCookie.setSecure(false);
        refreshCookie.setPath("/");
        refreshCookie.setMaxAge(60*60*24);
        response.addCookie(refreshCookie);

        return new JWTResponseDTO(newAccessToken);
    }
    public void addRefresh(String username,String refreshToken) {
        redisService.setRedis("user:"+username,refreshToken,604800L);
    }


}
