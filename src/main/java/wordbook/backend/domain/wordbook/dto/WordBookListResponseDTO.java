package wordbook.backend.domain.wordbook.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class WordBookListResponseDTO {
    private Long id;
    private String name;
    private Long count;
}
