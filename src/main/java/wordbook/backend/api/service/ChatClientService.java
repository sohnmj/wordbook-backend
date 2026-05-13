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
    private String systemMessage=
    """
You are a professional bilingual dictionary editor and language learning assistant.
Your task is to generate natural example sentences based on:
- target word
- meaning
- topic
Rules:
- Respond ONLY in valid JSON.
- Do NOT include markdown or explanations.
- The example sentence must sound natural and commonly used by native speakers.
- Avoid textbook-style expressions.
  The sentence must clearly reflect the given topic
- The meaning must match the usage in the sentence.
- Return only ONE example sentence.
- Translation must be written in Korean.
- Keep the sentence concise and practical.

    JSON format:
    {
        "word": "",
        "meaning": "",
        "topic": "",
        "example": "",
        "translation": ""
    } ;
""";

    @Override
    public WordResponseDTO getResponseME(String word, String lang, String meaning, String topic) {
        return null;
    }

    public ChatClientService(ChatClient.Builder builder, ObjectMapper objectMapper) {
        this.chatClient = builder.build();
        this.objectMapper = objectMapper;
    }
    @Override
    public WordResponseDTO getResponseEX(String word, String lang,String meaning,String topic) {

        String content=createContent(word,lang,meaning,topic);
        String response = callApi(content);
        ApiResponseDTO apiResponseDTO = objectMapper.readValue(response, ApiResponseDTO.class);
        return WordResponseDTO.builder()
                .word(apiResponseDTO.getMeaning())
                .example(apiResponseDTO.getExample())
                .meaning(apiResponseDTO.getWord())
                .translation(apiResponseDTO.getTranslation())
                .lang(lang)
                .build();
    }

    @Override
    public String createContent(String word, String lang,String meaning,String topic) {
        return """
The input word is "%s".
The meaning of the word is "%s".
The topic or situation is "%s".
The target language is %s.
Generate one natural and commonly used example sentence
that matches the given meaning and topic/situation
""".formatted(word, meaning,topic,lang);
    }


    public String callApi(String content){
        String text= chatClient.prompt()
                .system(systemMessage)
                .user(content)
                .call()
                .content();
        return text;
    }

}
