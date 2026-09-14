ALTER TABLE event
ADD COLUMN unvisited_street_penalty_percent INTEGER NOT NULL DEFAULT 150 CHECK (unvisited_street_penalty_percent >= 0);

ALTER TABLE game
ADD COLUMN unvisited_street_penalty_percent INTEGER NOT NULL DEFAULT 150 CHECK (unvisited_street_penalty_percent >= 0);
