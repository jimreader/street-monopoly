package com.streetmonopoly.mapper;

import com.streetmonopoly.dto.Dtos.ChallengeSubmissionAdminView;
import com.streetmonopoly.model.EventChallengeSubmission;
import org.apache.ibatis.annotations.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Mapper
public interface EventChallengeSubmissionMapper {

    @Select("SELECT * FROM event_challenge_submission WHERE challenge_id = #{challengeId} AND event_player_id = #{eventPlayerId}")
    EventChallengeSubmission findByChallengeAndEventPlayer(@Param("challengeId") UUID challengeId,
                                                           @Param("eventPlayerId") UUID eventPlayerId);

    @Select("SELECT ecs.* FROM event_challenge_submission ecs " +
            "JOIN event_challenge ec ON ec.id = ecs.challenge_id " +
            "WHERE ecs.id = #{submissionId} AND ecs.challenge_id = #{challengeId} AND ec.event_id = #{eventId} AND ec.deleted_at IS NULL")
    EventChallengeSubmission findInEvent(@Param("eventId") UUID eventId,
                                         @Param("challengeId") UUID challengeId,
                                         @Param("submissionId") UUID submissionId);

    @Insert("INSERT INTO event_challenge_submission (id, challenge_id, event_player_id, photo_url) VALUES (#{id}, #{challengeId}, #{eventPlayerId}, #{photoUrl})")
    void insert(EventChallengeSubmission submission);

    @Update("UPDATE event_challenge_submission SET review_status = #{reviewStatus}, review_notes = #{reviewNotes}, reviewed_at = #{reviewedAt}, prize_awarded_amount = #{prizeAwardedAmount} WHERE id = #{id}")
    int review(@Param("id") UUID id,
               @Param("reviewStatus") String reviewStatus,
               @Param("reviewNotes") String reviewNotes,
               @Param("reviewedAt") LocalDateTime reviewedAt,
               @Param("prizeAwardedAmount") BigDecimal prizeAwardedAmount);

    @Select("SELECT ecs.id AS submission_id, ecs.challenge_id, ecs.event_player_id, ep.player_id, p.name AS player_name, p.email AS player_email, " +
            "ecs.photo_url, ecs.submitted_at, ecs.review_status, ecs.review_notes, ecs.reviewed_at, ecs.prize_awarded_amount " +
            "FROM event_challenge_submission ecs " +
            "JOIN event_player ep ON ep.id = ecs.event_player_id " +
            "JOIN player p ON p.id = ep.player_id " +
            "JOIN event_challenge ec ON ec.id = ecs.challenge_id " +
            "WHERE ec.event_id = #{eventId} AND ecs.challenge_id = #{challengeId} AND ec.deleted_at IS NULL " +
            "ORDER BY ecs.submitted_at ASC")
    List<ChallengeSubmissionAdminView> findAdminSubmissions(@Param("eventId") UUID eventId,
                                                            @Param("challengeId") UUID challengeId);
}
