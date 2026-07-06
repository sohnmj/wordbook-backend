package wordbook.backend.domain.word.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class WordResponseDTO {
    private Long wordId;
    private String word;
    private String meaning;
    private String lang;
    private String example;
    private String topic;
    private String translation;
    private String useword;
    private int startIndex;
    private int lastIndex;
}
