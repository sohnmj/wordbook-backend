package wordbook.backend.api.service;

import wordbook.backend.api.ApiResponseDTO;
import wordbook.backend.domain.word.dto.WordResponseDTO;

public interface ApiService {
    public WordResponseDTO getResponse(String word, String lang) ;
    public String createContent(String word,String lang,boolean iskorean);
    public Boolean isKorean(String keyword);
}
