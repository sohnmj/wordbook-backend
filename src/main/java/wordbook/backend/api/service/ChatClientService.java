package wordbook.backend.api.service;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;
import wordbook.backend.api.ApiResponseDTO;
import wordbook.backend.domain.word.dto.WordResponseDTO;

@Service
@Primary
public class ChatClientService implements ApiService {
    private final ChatClient chatClient;
    private final ObjectMapper objectMapper;
    private String systemMessage ;
    public ChatClientService(ChatClient.Builder builder, ObjectMapper objectMapper) {
        this.chatClient = builder.build();
        this.objectMapper = objectMapper;
    }
   @Override
    public WordResponseDTO getResponse(String word, String lang) {
        Boolean iskorean = isKorean(word);
        if(iskorean) {
            systemMessage="""
You are a professional bilingual dictionary editor.

Respond ONLY with raw JSON.
No explanations.
No markdown.

Rules:
- The input word is Korean.
- Translate the base form into the target foreign language.
- Use ONLY the given JSON format.
- If there are multiple common meanings, list them in "meaning" using numbers.
- Meanings must be short and commonly used expressions in the target language.
- Provide only ONE representative example sentence in the target language.
- synonym and antonym must be true and appropriate.
- Do NOT use hypernyms.
- Do NOT force antonyms.
- If none exist, return an empty string "".
- "meaning" must be a dictionary headword, not an infinitive phrase.
- Verb meanings must be a single bare verb (one word only).
- "meaning" must never start with "to ".
- Outputs that violate this are invalid.

JSON format:
{
  "meaning": "",
  "example": "",
  "synonym": "",
  "antonym": ""
}
""";
        }
        else{
            systemMessage="""
You are a professional dictionary editor.

Respond ONLY with raw JSON.
No explanations.
No markdown.

Rules:
- Use ONLY the given JSON format.
- If there are multiple common meanings, list them in "meaning" using numbers.
- Meanings must be short Korean expressions.
- Provide only ONE representative example sentence.
- synonym and antonym must be true and appropriate.
- Do NOT use hypernyms.
- Do NOT force antonyms.
- If none exist, return an empty string "".

JSON format:
{
  "meaning": "",
  "example": "",
  "synonym": "",
  "antonym": ""
}
""";
        }
        String content=createContent(word,lang,iskorean);
        String response = callApi(content);
        ApiResponseDTO apiResponseDTO = objectMapper.readValue(response, ApiResponseDTO.class);
        if(isKorean(word)) {
            return WordResponseDTO.builder()
                    .word(getWordFromMeaning(apiResponseDTO.getMeaning()))
                    .example(apiResponseDTO.getExample())
                    .synonym(apiResponseDTO.getSynonym())
                    .antonym(apiResponseDTO.getAntonym())
                    .meaning(word)
                    .lang(lang)
                    .build();
        }
        return WordResponseDTO.builder()
                .word(word)
                .lang(lang)
                .meaning(apiResponseDTO.getMeaning())
                .example(apiResponseDTO.getExample())
                .synonym(apiResponseDTO.getSynonym())
                .antonym(apiResponseDTO.getAntonym())
                .build();

    }

    @Override
    public String createContent(String word, String lang,boolean iskorean) {
        if(iskorean) {
            return """
The input word is "%s" (Korean).
The target language is %s.
If the input is an inflected or conjugated form, normalize it to its dictionary base form.
  (e.g. 만들었다 → 만들다, 먹었어 → 먹다)
""".formatted(word, lang);
        }
        return """
    - The target word is "%s".
    - The language of the word is %s.
    - meaning must be written in Korean.
    - example, synonym, antonym must be written in %s.
    """.formatted(word, lang, lang);
    }
    @Override
    public Boolean isKorean(String keyword){
        if (keyword == null || keyword.isEmpty()) {
            return false;
        }
        return keyword.matches("^[가-힣]+$");
    }
    public String callApi(String content){
        String text= chatClient.prompt()
                .system(systemMessage)
                .user(content)
                .call()
                .content();
//        System.out.println("text = " + text);
        return text;
    }
    public String getWordFromMeaning(String meaning){
        return  meaning
                .replaceAll("^\\s*1\\.\\s*", "") // "1. " 제거
                .split("\\s+2\\.")[0];
    }

}
