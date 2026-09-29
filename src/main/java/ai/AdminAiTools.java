package ai;

import dto.response.DailyStatsDto;
import dto.response.UserResponseDto;
import dto.response.WeeklyStatsDto;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;
import repository.NoteRepository;
import repository.UserRepository;

import java.time.*;
import java.util.*;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class AdminAiTools {

    private final UserRepository userRepository;
    private final NoteRepository noteRepository;

    @Tool(description = "Находит всех пользователей, зарегистрированных в определённую дату. Формат даты YYYY-MM-DD, например 2026-09-26")
    public List<UserResponseDto> getAllUsersByDate(
            @ToolParam(description = "Дата в формате YYYY-MM-DD") String date
    ) {
        LocalDate targetDate = LocalDate.parse(date);
        ZonedDateTime startOfDay = targetDate.atStartOfDay(ZoneId.systemDefault());
        ZonedDateTime endOfDay = targetDate.atTime(LocalTime.MAX).atZone(ZoneId.systemDefault());

        return userRepository.findByCreatedAtBetween(startOfDay, endOfDay).stream()
                .map(user -> UserResponseDto.builder()
                        .id(user.getId())
                        .username(user.getUsername())
                        .email(user.getEmail())
                        .role(user.getRole())
                        .noteCount(user.getNoteCount())
                        .createdAt(user.getCreatedAt())
                        .build())
                .toList();
    }

    @Tool(description = "Возвращает общее количество зарегистрированных пользователей в системе.")
    public long getTotalUsersCount() {
        return userRepository.count();
    }

    @Tool(description = "Возвращает агрегированную статистику создания заметок по 7-дневным интервалам (неделям) за указанный период.")
    public List<WeeklyStatsDto> getWeeklyNotesStats(
            @ToolParam(description = "Дата начала периода в формате YYYY-MM-DD") String startDate,
            @ToolParam(description = "Дата конца периода в формате YYYY-MM-DD") String endDate
    ) {
        LocalDate startLocalDate = LocalDate.parse(startDate);
        LocalDate endLocalDate = LocalDate.parse(endDate);

        ZonedDateTime startAnd = startLocalDate.atStartOfDay(ZoneId.systemDefault());
        ZonedDateTime endAnd = endLocalDate.atTime(23, 59, 59, 999_999_999).atZone(ZoneId.systemDefault());

        List<DailyStatsDto> dailyStats = noteRepository.getDailyNotesStats(startAnd, endAnd);

        Map<LocalDate, Long> dailyMap = dailyStats.stream()
                .collect(Collectors.toMap(DailyStatsDto::date, DailyStatsDto::count));

        List<WeeklyStatsDto> weeklyResults = new ArrayList<>();
        LocalDate currentEnd = endLocalDate;

        while (!currentEnd.isBefore(startLocalDate)) {
            LocalDate currentStart = currentEnd.minusDays(6);
            if (currentStart.isBefore(startLocalDate)) {
                currentStart = startLocalDate;
            }

            long sum = 0;
            for (LocalDate d = currentStart; !d.isAfter(currentEnd); d = d.plusDays(1)) {
                sum += dailyMap.getOrDefault(d, 0L);
            }

            weeklyResults.add(new WeeklyStatsDto(currentStart, currentEnd, sum));
            currentEnd = currentStart.minusDays(1);
        }

        Collections.reverse(weeklyResults);
        return weeklyResults;
    }
}