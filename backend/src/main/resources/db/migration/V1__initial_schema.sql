CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- Game Maps
CREATE TABLE game_map (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name VARCHAR(255) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

-- Streets belonging to a game map
CREATE TABLE street (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    game_map_id UUID NOT NULL REFERENCES game_map(id) ON DELETE CASCADE,
    name VARCHAR(255) NOT NULL,
    price DECIMAL(10,2) NOT NULL,
    rental_price DECIMAL(10,2) NOT NULL,
    colour VARCHAR(50) NOT NULL CHECK (colour IN (
        'brown', 'light_blue', 'pink', 'orange',
        'red', 'yellow', 'green', 'dark_blue'
    )),
    latitude DOUBLE PRECISION NOT NULL,
    longitude DOUBLE PRECISION NOT NULL,
    image_clue_url TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

-- Players
CREATE TABLE player (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

-- Games
CREATE TABLE game (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name VARCHAR(255) NOT NULL,
    game_map_id UUID NOT NULL REFERENCES game_map(id),
    start_time TIMESTAMP NOT NULL,
    end_time TIMESTAMP NOT NULL,
    starting_balance DECIMAL(10,2) NOT NULL,
    proximity_metres INTEGER NOT NULL DEFAULT 50,
    status VARCHAR(20) NOT NULL DEFAULT 'pending' CHECK (status IN ('pending', 'active', 'completed')),
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

-- Game-Player link (invitation + join)
CREATE TABLE game_player (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    game_id UUID NOT NULL REFERENCES game(id) ON DELETE CASCADE,
    player_id UUID NOT NULL REFERENCES player(id) ON DELETE CASCADE,
    balance DECIMAL(10,2) NOT NULL,
    invite_token UUID NOT NULL DEFAULT uuid_generate_v4(),
    join_token UUID NOT NULL DEFAULT uuid_generate_v4(),
    invited_at TIMESTAMP NOT NULL DEFAULT NOW(),
    joined_at TIMESTAMP,
    final_balance DECIMAL(10,2),
    UNIQUE(game_id, player_id)
);

-- Street ownership / visits during a game
CREATE TABLE game_street (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    game_id UUID NOT NULL REFERENCES game(id) ON DELETE CASCADE,
    street_id UUID NOT NULL REFERENCES street(id) ON DELETE CASCADE,
    owner_player_id UUID REFERENCES player(id),
    purchased_at TIMESTAMP,
    UNIQUE(game_id, street_id)
);

-- Visit records (when a player checks in at a street)
CREATE TABLE street_visit (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    game_id UUID NOT NULL REFERENCES game(id) ON DELETE CASCADE,
    street_id UUID NOT NULL REFERENCES street(id) ON DELETE CASCADE,
    player_id UUID NOT NULL REFERENCES player(id) ON DELETE CASCADE,
    visit_type VARCHAR(20) NOT NULL CHECK (visit_type IN ('purchased', 'rent_paid', 'insufficient_funds')),
    amount DECIMAL(10,2),
    visited_at TIMESTAMP NOT NULL DEFAULT NOW(),
    UNIQUE(game_id, street_id, player_id)
);

-- Index for performance
CREATE INDEX idx_game_player_invite ON game_player(invite_token);
CREATE INDEX idx_game_player_join ON game_player(join_token);
CREATE INDEX idx_game_street_game ON game_street(game_id);
CREATE INDEX idx_street_visit_player ON street_visit(game_id, player_id);
CREATE INDEX idx_game_status ON game(status);
