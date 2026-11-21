package wordbook.backend.domain.word.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record WordSearchRequestDTO(
        @NotBlank(message = "단어를 입력해 주세요.")
        @Size(max = 50, message = "50자 이하로 입력해 주세요.")
        String keyword,

        @NotBlank(message = "언어를 선택해 주세요.")
        String lang,

        @Size(max = 100, message = "100자 이하로 입력해 주세요.")
        String meaning,

        @Size(max = 100, message = "100자 이하로 입력해 주세요.")
        String topic
) {};