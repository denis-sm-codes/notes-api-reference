package ai;

import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AiService {

    private final AiProperties aiProperties;
    private final RestClient restClient = RestClient.create();

    public String processAdminQuery(String userQuery) {
        String currentZonedDateTime = ZonedDateTime.now().toString();
        String systemInstruction = String.format(
                "Ты — AI-помощник админа. Текущие дата и время сервера: %s. " +
                        "Учитывай время при расчёте относительных временных интервалов " +
                        "('последний час', 'сегодня с утра', 'вчера после 18:00' и т.д.). " +
                        "При вызове инструментов передавай даты и интервалы в требуемом формате.",
                currentZonedDateTime
        );

        return askGemini(systemInstruction, userQuery);
    }

    public String askGemini(String systemInstruction, String prompt) {
        String fullUrl = aiProperties.getUrl() + "?key=" + aiProperties.getKey();

        var requestBody = Map.of(
                "system_instruction", Map.of(
                        "parts", List.of(Map.of("text", systemInstruction))
                ),
                "contents", List.of(
                        Map.of("role", "user", "parts", List.of(Map.of("text", prompt)))
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