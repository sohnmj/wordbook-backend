package wordbook.backend.api.service;

import wordbook.backend.api.ApiResponseDTO;
import wordbook.backend.domain.word.dto.WordResponseDTO;
import wordbook.backend.domain.word.dto.WordSearchRequestDTO;

public interface ApiService {
    public WordResponseDTO getResponseEX(WordSearchRequestDTO wordSearchRequestDTO);
    public WordResponseDTO getResponseME(String word, String lang,String meaning,String topic);
    public String createContent(String word, String lang,String meaning,String topic);
}
