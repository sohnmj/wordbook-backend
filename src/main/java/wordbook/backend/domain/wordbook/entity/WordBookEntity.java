package wordbook.backend.domain.wordbook.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import wordbook.backend.domain.user.entity.UserEntity;
import wordbook.backend.domain.wordbookword.entity.WordBookWordEntity;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name="wordbook")
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class WordBookEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity userEntity;

    @Column(name="word_book_name",nullable = false)
    private String name;

    @Builder.Default
    @Column(name="like_count")
    private Long likeCount=0l;

    @Builder.Default
    @Column(name="word_count" )
    private Long wordCount=0l;

    @OneToMany(mappedBy = "wordBookEntity", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<WordBookWordEntity> wordBookWords = new ArrayList<>();

    public void incrementLikeCount(){
        likeCount++;
    }
    public void decrementLikeCount(){
        likeCount--;
    }
    public void incrementWordCount(){
        wordCount++;
    }
    public void decrementWordCount(){
        wordCount--;
    }
}
