package wordbook.backend.domain.wordbook.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import wordbook.backend.domain.word.dto.WordResponseDTO;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
public class WordBookResponseDTO {
    List<WordResponseDTO> words;
    String name;
}
