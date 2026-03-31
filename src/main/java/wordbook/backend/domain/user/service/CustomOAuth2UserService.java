package wordbook.backend.domain.user.service;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import wordbook.backend.domain.user.dto.CustomOAuth2User;
import wordbook.backend.domain.user.entity.SocialProviderType;
import wordbook.backend.domain.user.entity.UserEntity;
import wordbook.backend.domain.user.entity.UserRoleType;
import wordbook.backend.domain.user.repository.UserRepository;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class CustomOAuth2UserService extends DefaultOAuth2UserService {
    private final UserRepository userRepository;
    public CustomOAuth2UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }
    @Transactional
    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(userRequest);

        Map<String, Object> attributes;
        List<GrantedAuthority> authorities;

        String username;
        String role = UserRoleType.USER.name();
        String email;
        String nickname;


        String registrationId = userRequest.getClientRegistration().getRegistrationId().toUpperCase();
         if (registrationId.equals(SocialProviderType.GOOGLE.name())) {

            attributes = (Map<String, Object>) oAuth2User.getAttributes();
            username = registrationId + "_" + attributes.get("sub");
            email = attributes.get("email").toString();
            nickname = attributes.get("name").toString();
        }
        else{
            throw new OAuth2AuthenticationException("지원하지 않은 소셜 로그인");
        }
        Optional<UserEntity> user=userRepository.findByUsernameAndIsSocial(username,true);
        if(user.isPresent()){
            // role 조회
            role = user.get().getRole().name();

            // 기존 유저 업데이트
            user.get().update(email);

            userRepository.save(user.get());
        }
        else{
            UserEntity newUserEntity=UserEntity.builder()
                    .username(username)
                    .password("")
                    .isSocial(true)
                    .social(SocialProviderType.valueOf(registrationId))
                    .role(UserRoleType.USER)
                    .email(email)
                    .build();
            userRepository.save(newUserEntity);
        }
        authorities = List.of(new SimpleGrantedAuthority(role));
        return new CustomOAuth2User(attributes,authorities,username);
    }
}
