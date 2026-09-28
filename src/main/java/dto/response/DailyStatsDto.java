package dto.response;

import java.time.LocalDate;

public record DailyStatsDto(

        LocalDate date,

        long count
) {}