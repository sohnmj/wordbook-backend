package wordbook.backend.api.service;

import wordbook.backend.api.ApiResponseDTO;
import wordbook.backend.domain.word.dto.WordResponseDTO;

public interface ApiService {
    public WordResponseDTO getResponseEX(String word, String lang,String meaning,String topic);
    public WordResponseDTO getResponseME(String word, String lang,String meaning,String topic);
    public String createContent(String word, String lang,String meaning,String topic);
}
