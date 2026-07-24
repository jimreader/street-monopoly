CREATE TABLE event (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name VARCHAR(255) NOT NULL,
    game_map_id UUID NOT NULL REFERENCES game_map(id),
    start_time TIMESTAMP NOT NULL,
    end_time TIMESTAMP NOT NULL,
    starting_balance DECIMAL(10,2) NOT NULL,
    proximity_metres INTEGER NOT NULL DEFAULT 50,
    max_players_per_game INTEGER NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'pending' CHECK (status IN ('pending', 'active', 'completed')),
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    deleted_at TIMESTAMP
);

CREATE TABLE event_player (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    event_id UUID NOT NULL REFERENCES event(id) ON DELETE CASCADE,
    player_id UUID NOT NULL REFERENCES player(id) ON DELETE CASCADE,
    assigned_game_id UUID REFERENCES game(id),
    invite_token UUID NOT NULL DEFAULT uuid_generate_v4(),
    join_token UUID NOT NULL DEFAULT uuid_generate_v4(),
    invited_at TIMESTAMP NOT NULL DEFAULT NOW(),
    joined_at TIMESTAMP,
    device_token VARCHAR(64),
    deleted_at TIMESTAMP
);

ALTER TABLE game
ADD COLUMN event_id UUID REFERENCES event(id);

ALTER TABLE game_player
ADD COLUMN event_player_id UUID REFERENCES event_player(id);

CREATE UNIQUE INDEX uq_event_player_active
ON event_player (event_id, player_id)
WHERE deleted_at IS NULL;

CREATE INDEX idx_event_status ON event(status);
CREATE INDEX idx_event_player_join ON event_player(join_token);
CREATE INDEX idx_game_event_id ON game(event_id);
CREATE INDEX idx_game_player_event_player_id ON game_player(event_player_id);
