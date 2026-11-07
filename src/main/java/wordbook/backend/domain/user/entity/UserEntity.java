package wordbook.backend.domain.user.entity;


import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import wordbook.backend.domain.word.entity.WordEntity;
import wordbook.backend.domain.wordbook.entity.WordBookEntity;
import wordbook.backend.domain.wordbookword.entity.WordBookWordEntity;

import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name="user")
public class UserEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name="username", unique = true, nullable=false)
    private String username;

    @Column(name="password", nullable = false)
    private String password;

    @Column(name="email",unique = true)
    private String email;

    @Column(name="is_social", nullable=false)
    private Boolean isSocial;

    @Column(name="api_usage", nullable=false)
    private int usage;

    @OneToMany(mappedBy = "userEntity", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<WordBookEntity> wordBookWords = new ArrayList<>();

    @OneToMany(mappedBy = "userEntity", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<WordEntity> words = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    @Column(name="role", nullable=false)
    private UserRoleType role;

    @Enumerated(EnumType.STRING)
    @Column(name="social")
    private SocialProviderType social;
    public static final int DAILY_GRANT=10;
//    public void updateUsage(int usage) {
//        this.usage =this.usage+ usage;
//    }
    //24시간마다 10 사용량을 얻을 수 있음
    public void updateDailyUsage() {
        this.usage =this.usage+ DAILY_GRANT;
    }
    //사용 가능 여부 (잔량 > 0)
    public boolean canUse() {
        return this.usage > 0;
    }
    //사용량 사용 (실제 요청 경로는 동시성 때문에 UserRepository.consumeUsage 쿼리를 사용)
    public void consumeUsage(){
        if(this.usage<=0){
            throw new IllegalStateException("일일 이용 횟수를 모두 소진하였습니다.");
        }
        this.usage--;
    }
    public void update(String email){
        this.email=email;
    }
}
