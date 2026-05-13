package wordbook.backend.domain.wordbookcomment.dto;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WordBookCommentResponseDTO {
    private String comment;
    private Long userId;
    private String userName;
}
