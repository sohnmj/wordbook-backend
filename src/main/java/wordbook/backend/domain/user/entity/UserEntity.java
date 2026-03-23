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
    public void setId(Long id){
        this.id=id;
    }
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
    private boolean IsSocial;
    @Column(name="api_usage", nullable=false)
    private int usage;
    @OneToMany(mappedBy = "userEntity", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<WordBookEntity> wordBookWords = new ArrayList<>();
    @OneToMany(mappedBy = "userEntity", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<WordEntity> words = new ArrayList<>();
    public void updateUsage(int usage) {
        this.usage =this.usage+ usage;
    }
}
