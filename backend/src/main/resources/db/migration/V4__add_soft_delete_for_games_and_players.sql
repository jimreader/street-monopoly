ALTER TABLE game
ADD COLUMN deleted_at TIMESTAMP;

ALTER TABLE game_player
ADD COLUMN deleted_at TIMESTAMP;

ALTER TABLE game_player
DROP CONSTRAINT IF EXISTS game_player_game_id_player_id_key;

CREATE UNIQUE INDEX uq_game_player_active
ON game_player (game_id, player_id)
WHERE deleted_at IS NULL;
