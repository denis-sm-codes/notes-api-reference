package repository;

import dto.response.DailyStatsDto;
import entity.Note;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;

public interface NoteRepository extends JpaRepository<Note, Long> {

    Page<Note> findAllByUserId(Long userId, Pageable pageable);

    Optional<Note> findByIdAndUserId(Long id, Long userId);

    void deleteByIdAndUserId(Long id, Long userId);

    boolean existsByIdAndUserId(Long id, Long userId);

    @Query("""
    SELECT new dto.response.DailyStatsDto(
        CAST(n.createdAt AS LocalDate), 
        COUNT(n)
    )
    FROM Note n
    WHERE n.createdAt BETWEEN :startAnd AND :endAnd
    GROUP BY CAST(n.createdAt AS LocalDate)
    ORDER BY CAST(n.createdAt AS LocalDate) ASC
""")
    List<DailyStatsDto> getDailyNotesStats(
            @Param("startAnd") ZonedDateTime startAnd,
            @Param("endAnd") ZonedDateTime endAnd
    );
}
