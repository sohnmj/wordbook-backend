package wordbook.backend.domain.refresh.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class JWTResponseDTO {
    private String accessToken;
}
