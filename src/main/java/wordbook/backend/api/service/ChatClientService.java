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
    private String systemMessage =
            """
            You are a professional bilingual dictionary editor and language learning assistant.
            
            Your task is to generate a natural example sentence based on:
            - target word
            - meaning
            - topic
            
            Rules:
            - Respond ONLY in valid JSON.
            - Do NOT include markdown, explanations, or extra text.
            - Generate ONLY ONE example sentence.
            - The sentence must sound natural and commonly used by native speakers.
            - Avoid textbook-style expressions.
            - The sentence must clearly reflect the given topic.
            - The meaning must match the usage in the sentence.
            - Keep the sentence concise and practical.
            - Translation must be written in Korean.
            - The target word may appear in an inflected form.
            - Include the actual word form used in the sentence as "usedWord".
            - The target word or used word must appear exactly once in the example sentence.
            
            Return JSON in the following format:
            
            {
              "word": "",
              "useword": "",
              "meaning": "",
              "topic": "",
              "example": "",
              "translation": ""
            }
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
                .meaning(apiResponseDTO.getMeaning())
                .example(apiResponseDTO.getExample())
                .word(apiResponseDTO.getWord())
                .translation(apiResponseDTO.getTranslation())
                .lang(lang)
                .useword(apiResponseDTO.getUseword())
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
