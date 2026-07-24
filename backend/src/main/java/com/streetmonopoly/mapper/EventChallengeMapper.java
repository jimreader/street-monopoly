package com.streetmonopoly.mapper;

import com.streetmonopoly.dto.Dtos.ChallengeAdminView;
import com.streetmonopoly.dto.Dtos.PlayerChallengeView;
import com.streetmonopoly.model.EventChallenge;
import org.apache.ibatis.annotations.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Mapper
public interface EventChallengeMapper {

    @Select("SELECT * FROM event_challenge WHERE event_id = #{eventId} AND deleted_at IS NULL ORDER BY created_at ASC")
    List<EventChallenge> findByEventId(UUID eventId);

    @Select("SELECT * FROM event_challenge WHERE event_id = #{eventId} AND id = #{challengeId} AND deleted_at IS NULL")
    EventChallenge findByEventAndId(@Param("eventId") UUID eventId, @Param("challengeId") UUID challengeId);

    @Insert("INSERT INTO event_challenge (id, event_id, description, prize_amount, duration_minutes, status) VALUES (#{id}, #{eventId}, #{description}, #{prizeAmount}, #{durationMinutes}, #{status})")
    void insert(EventChallenge challenge);

    @Update("UPDATE event_challenge SET description = #{description}, prize_amount = #{prizeAmount}, duration_minutes = #{durationMinutes}, updated_at = NOW() WHERE id = #{id} AND event_id = #{eventId} AND deleted_at IS NULL")
    int updateDraft(EventChallenge challenge);

    @Update("UPDATE event_challenge SET deleted_at = NOW(), updated_at = NOW() WHERE id = #{challengeId} AND event_id = #{eventId} AND deleted_at IS NULL")
    int softDelete(@Param("eventId") UUID eventId, @Param("challengeId") UUID challengeId);

    @Select("SELECT COALESCE(SUM(duration_minutes), 0) FROM event_challenge WHERE event_id = #{eventId} AND deleted_at IS NULL")
    int sumDurationsByEvent(UUID eventId);

    @Select("SELECT COALESCE(SUM(duration_minutes), 0) FROM event_challenge WHERE event_id = #{eventId} AND deleted_at IS NULL AND id <> #{excludeId}")
    int sumDurationsByEventExcluding(@Param("eventId") UUID eventId, @Param("excludeId") UUID excludeId);

    @Update("UPDATE event_challenge SET scheduled_start_at = #{scheduledStartAt}, scheduled_end_at = #{scheduledEndAt}, updated_at = NOW() WHERE id = #{id} AND deleted_at IS NULL")
    void setSchedule(@Param("id") UUID id,
                     @Param("scheduledStartAt") LocalDateTime scheduledStartAt,
                     @Param("scheduledEndAt") LocalDateTime scheduledEndAt);

    @Select("SELECT * FROM event_challenge WHERE deleted_at IS NULL AND status = 'pending' AND scheduled_start_at IS NOT NULL AND scheduled_start_at <= NOW()")
    List<EventChallenge> findPendingReadyToActivate();

    @Select("SELECT * FROM event_challenge WHERE deleted_at IS NULL AND status = 'active' AND scheduled_end_at IS NOT NULL AND scheduled_end_at <= NOW()")
    List<EventChallenge> findActiveReadyToComplete();

    @Update("UPDATE event_challenge SET status = #{status}, updated_at = NOW() WHERE id = #{id} AND deleted_at IS NULL")
    void updateStatus(@Param("id") UUID id, @Param("status") String status);

    @Select("SELECT c.id, c.event_id, c.description, c.prize_amount, c.duration_minutes, c.scheduled_start_at, c.scheduled_end_at, c.status, c.created_at, " +
            "COUNT(s.id) AS submitted_count, " +
            "COALESCE(SUM(CASE WHEN s.review_status = 'accomplished' THEN 1 ELSE 0 END), 0) AS accomplished_count, " +
            "COALESCE(SUM(CASE WHEN s.review_status = 'failed' THEN 1 ELSE 0 END), 0) AS failed_count, " +
            "COALESCE(SUM(CASE WHEN s.review_status = 'pending' THEN 1 ELSE 0 END), 0) AS pending_review_count " +
            "FROM event_challenge c " +
            "LEFT JOIN event_challenge_submission s ON s.challenge_id = c.id " +
            "WHERE c.event_id = #{eventId} AND c.deleted_at IS NULL " +
            "GROUP BY c.id " +
            "ORDER BY c.created_at ASC")
    List<ChallengeAdminView> findAdminViewsByEventId(UUID eventId);

    @Select("SELECT c.id, c.description, c.prize_amount, c.duration_minutes, c.scheduled_start_at, c.scheduled_end_at, c.status, " +
            "CASE " +
            "WHEN c.status = 'active' AND s.id IS NULL THEN 'awaiting_submission' " +
            "WHEN c.status = 'active' AND s.review_status = 'pending' THEN 'submitted_pending_review' " +
            "WHEN c.status = 'active' THEN CONCAT('submitted_', s.review_status) " +
            "WHEN c.status = 'completed' AND s.id IS NULL THEN 'missed' " +
            "WHEN c.status = 'completed' AND s.review_status = 'pending' THEN 'submitted_pending_review' " +
            "ELSE CONCAT('submitted_', s.review_status) " +
            "END AS submission_status, " +
            "s.photo_url AS submitted_photo_url, s.submitted_at, s.review_status, s.review_notes, s.reviewed_at " +
            "FROM event_challenge c " +
            "LEFT JOIN event_challenge_submission s ON s.challenge_id = c.id AND s.event_player_id = #{eventPlayerId} " +
            "WHERE c.event_id = #{eventId} AND c.deleted_at IS NULL AND c.status <> 'pending' " +
            "ORDER BY c.created_at ASC")
    List<PlayerChallengeView> findPlayerViews(@Param("eventId") UUID eventId, @Param("eventPlayerId") UUID eventPlayerId);
}
