CREATE TABLE event_challenge (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    event_id UUID NOT NULL REFERENCES event(id) ON DELETE CASCADE,
    description TEXT NOT NULL,
    prize_amount DECIMAL(10,2) NOT NULL,
    duration_minutes INTEGER NOT NULL CHECK (duration_minutes > 0),
    scheduled_start_at TIMESTAMP,
    scheduled_end_at TIMESTAMP,
    status VARCHAR(20) NOT NULL DEFAULT 'pending' CHECK (status IN ('pending', 'active', 'completed')),
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    deleted_at TIMESTAMP
);

CREATE TABLE event_challenge_submission (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    challenge_id UUID NOT NULL REFERENCES event_challenge(id) ON DELETE CASCADE,
    event_player_id UUID NOT NULL REFERENCES event_player(id) ON DELETE CASCADE,
    photo_url TEXT NOT NULL,
    submitted_at TIMESTAMP NOT NULL DEFAULT NOW(),
    review_status VARCHAR(20) NOT NULL DEFAULT 'pending' CHECK (review_status IN ('pending', 'accomplished', 'failed')),
    review_notes TEXT,
    reviewed_at TIMESTAMP,
    prize_awarded_amount DECIMAL(10,2) NOT NULL DEFAULT 0,
    UNIQUE(challenge_id, event_player_id)
);

CREATE INDEX idx_event_challenge_event_id ON event_challenge(event_id);
CREATE INDEX idx_event_challenge_status ON event_challenge(status);
CREATE INDEX idx_event_challenge_schedule ON event_challenge(scheduled_start_at, scheduled_end_at);
CREATE INDEX idx_event_challenge_submission_challenge_id ON event_challenge_submission(challenge_id);
CREATE INDEX idx_event_challenge_submission_event_player_id ON event_challenge_submission(event_player_id);
