package wordbook.backend.domain.wordbookcomment.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import wordbook.backend.domain.user.entity.UserEntity;
import wordbook.backend.domain.wordbook.entity.WordBookEntity;

@Entity
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WordBookCommentEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name="content",nullable = false)
    private String content;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="user_id")
    private UserEntity userEntity;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="parent_id")
    private WordBookCommentEntity parentEntity;

    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="wordbook_id")
    private WordBookEntity wordBookEntity;

}
