package dto.response;

import java.time.LocalDate;

public record WeeklyStatsDto(

        LocalDate startDate,

        LocalDate endDate,

        long count
) {}