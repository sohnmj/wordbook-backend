package wordbook.backend.domain.wordbookcomment.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class WordBookCommentRequestDTO {
    private String comment;
    private Long parentId;
    private String username;
    private Long wordBookId;
}
