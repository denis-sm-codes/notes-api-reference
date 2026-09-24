package ai;

import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AiService {

    private final AiProperties aiProperties;
    private final RestClient restClient = RestClient.create();

    public String askGemini(String prompt) {
        String fullUrl = aiProperties.getUrl() + "?key=" + aiProperties.getKey();

        var requestBody = Map.of(
                "contents", List.of(
                        Map.of("parts", List.of(
                                Map.of("text", prompt)
                        ))
                )
        );

        GeminiResponse response = restClient.post()
                .uri(fullUrl)
                .contentType(MediaType.APPLICATION_JSON)
                .body(requestBody)
                .retrieve()
                .body(GeminiResponse.class);

        return response != null ? response.getFirstCandidateText() : "Ошибка получения ответа";
    }
}