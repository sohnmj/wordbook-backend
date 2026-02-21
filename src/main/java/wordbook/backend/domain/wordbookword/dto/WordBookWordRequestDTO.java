package wordbook.backend.domain.wordbookword.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class WordBookWordRequestDTO {
    private Long word;
    private Long wordbook;
}
