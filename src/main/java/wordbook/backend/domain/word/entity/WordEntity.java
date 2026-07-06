package wordbook.backend.domain.word.entity;

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
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@Table(name="word")
public class WordEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity userEntity;

    @Column(name = "word", nullable = false)
    private String word;

    @Column(name = "meaning", nullable = false)
    private String meaning;

    @Column(name="lang",nullable = false)
    private String lang;

    @Column(name = "example")
    private String example;

    @Column(name = "topic")
    private String topic;

    @Column(name = "translation")
    private String translation;

    @Column(name = "useword")
    private String useword;

    @Column(name="example_start_index")
    private int exampleStartIndex;

    @Column(name="example_last_index")
    private int exampleLastIndex;

    @OneToMany(mappedBy = "wordEntity", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<WordBookWordEntity> wordBookWords = new ArrayList<>();
}