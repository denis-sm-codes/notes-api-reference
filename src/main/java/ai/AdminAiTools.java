package ai;

import dto.response.DailyStatsDto;
import dto.response.UserResponseDto;
import dto.response.WeeklyStatsDto;
import jdk.jfr.Description;
import lombok.AllArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;
import repository.NoteRepository;
import repository.UserRepository;

import java.time.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;


@Component
@AllArgsConstructor
public class AdminAiTools {

    private final UserRepository userRepository;
    private final NoteRepository noteRepository;

    @Tool(description = "Находит всех пользователей, зарегестрированных в определённую дату. Формат даты YYYY-MM-DD, например 2026-9-26")
    private List<UserResponseDto> getAllUsersByDate(String date){
        LocalDate targetDate = LocalDate.parse(date);

        ZonedDateTime startOfDay = targetDate.atStartOfDay(ZoneId.systemDefault());
        ZonedDateTime endOfDay = targetDate.atTime(LocalTime.MAX).atZone(ZoneId.systemDefault());

        return userRepository.findByCreatedAtBetween(startOfDay, endOfDay).stream().map(user ->
                UserResponseDto.builder()
                        .id(user.getId())
                        .username(user.getUsername())
                        .email(user.getEmail())
                        .role(user.getRole())
                        .noteCount(user.getNoteCount())
                        .createdAt(user.getCreatedAt())
                        .build()).toList();
    }

    @Tool(description = "Возвращает общее количество зарегистрированных пользователей в системе.")
    public long getTotalUsersCount() {
        return userRepository.count();
    }

    public record WeeklyStatsRequest(
            @Description("Дата начала периода в формате YYYY-MM-DD") String startDate,
            @Description("Дата конца периода в формате YYYY-MM-DD (обычно сегодняшнее число)") String endDate
    ) {}

    @Bean
    @Description("Возвращает агрегированную статистику создания заметок по 7-дневным интервалам (неделям) за указанный период. Используется для анализа трендов активности пользователей.")
    public Function<WeeklyStatsRequest, List<WeeklyStatsDto>> getWeeklyNotesStats() {
        return request -> {
            // 1. Парсим входящие даты ISO-8601 и задаем полные временные границы дня
            LocalDate startLocalDate = LocalDate.parse(request.startDate());
            LocalDate endLocalDate = LocalDate.parse(request.endDate());

            ZonedDateTime startAnd = startLocalDate.atStartOfDay(ZoneId.systemDefault());
            ZonedDateTime endAnd = endLocalDate.atTime(23, 59, 59, 999_999_999).atZone(ZoneId.systemDefault());

            // 2. Делаем 1 быстрый запрос к БД и получаем дневные агрегаты
            List<DailyStatsDto> dailyStats = noteRepository.getDailyNotesStats(startAnd, endAnd);

            // 3. Превращаем список в Map<LocalDate, Long> для мгновенного поиска O(1)
            Map<LocalDate, Long> dailyMap = dailyStats.stream()
                    .collect(Collectors.toMap(DailyStatsDto::date, DailyStatsDto::count));

            // 4. Сворачиваем дни в 7-дневные блоки, отсчитывая назад от endLocalDate ("сегодня")
            List<WeeklyStatsDto> weeklyResults = new ArrayList<>();
            LocalDate currentEnd = endLocalDate;

            while (!currentEnd.isBefore(startLocalDate)) {
                LocalDate currentStart = currentEnd.minusDays(6); // Интервал в 7 дней [Start..End]

                // Корректируем границу, если ушли за пределы запрошенного startLocalDate
                if (currentStart.isBefore(startLocalDate)) {
                    currentStart = startLocalDate;
                }

                // Суммируем счетчики за эти 7 дней
                long sum = 0;
                for (LocalDate date = currentStart; !date.isAfter(currentEnd); date = date.plusDays(1)) {
                    sum += dailyMap.getOrDefault(date, 0L);
                }

                weeklyResults.add(new WeeklyStatsDto(currentStart, currentEnd, sum));

                // Сдвигаемся на следующий (предыдущий) 7-дневный блок
                currentEnd = currentStart.minusDays(1);
            }

            // Разворачиваем, чтобы отдать данные в хронологическом порядке (от старых к новым)
            Collections.reverse(weeklyResults);

            return weeklyResults;
        };
    }

}
