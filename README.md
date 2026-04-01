# Street Monopoly 🏙️

A real-world location-based Monopoly game where players physically visit streets to purchase them. Built as three components: a Spring Boot REST API, an admin React app, and a player React app.

## Architecture

```
street-monopoly/
├── backend/          Spring Boot 3.2 + MyBatis + PostgreSQL
├── admin-app/        React + Vite (port 3000) — game administration
└── player-app/       React + Vite (port 3001) — mobile player experience
```

## Prerequisites

- Java 17+
- Maven 3.8+
- Node.js 18+
- PostgreSQL 14+

## Database Setup

```bash
# Create the database
psql -U postgres -c "CREATE DATABASE street_monopoly;"
```

Flyway will automatically run the migration (`V1__initial_schema.sql`) on first startup.

## Backend Setup

```bash
cd backend

# Configure database connection (edit if your credentials differ)
# Default: postgres/postgres on localhost:5432/street_monopoly

# Configure email (optional — emails are logged to console if not configured)
# Set environment variables:
#   MAIL_USERNAME=your-email@gmail.com
#   MAIL_PASSWORD=your-app-password

# Build and run
mvn spring-boot:run
```

The API starts on **http://localhost:8080**.

## Admin App Setup

```bash
cd admin-app
npm install
npm run dev
```

Opens on **http://localhost:3000**. Proxies API calls to the backend.

## Player App Setup

```bash
cd player-app
npm install
npm run dev
```

Opens on **http://localhost:3001**. Proxies API calls to the backend.

---

## How It Works

### Admin Workflow

1. **Create a Game Map** — give it a name, then add streets with prices, rental costs, Monopoly colours, GPS coordinates, and an image clue.
2. **Create a Game** — pick a map, set start/end times, starting balance, and proximity distance (how close a player must be to check in).
3. **Invite Players** — enter name and email. An invite email is sent with an acceptance link.
4. **Monitor the Game** — view street ownership, player balances, and a live leaderboard.

### Player Workflow

1. **Accept Invite** — click the link in the invitation email. This triggers a second email with the game join link.
2. **Wait for Start** — the player app shows a countdown until the game begins.
3. **Play the Game** — when the game starts, players see all streets with image clues. They physically travel to street locations. When close enough (GPS-based), a "Check In" button appears:
   - **Street unowned + sufficient funds** → player purchases it (price deducted from balance)
   - **Street unowned + insufficient funds** → street marked as visited, no purchase
   - **Street owned by another player** → rent is paid (deducted from player, added to owner)
4. **Game Ends** — rental values for all unvisited streets are deducted from the player's balance to compute the final score. The player with the highest final balance wins.

### Game Status Lifecycle

- **Pending** → waiting for start time (players can be invited)
- **Active** → game in progress (checked every 15 seconds by scheduled task)
- **Completed** → game ended, final balances calculated

---

## API Endpoints

### Game Maps
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/maps` | List all maps |
| GET | `/api/maps/:id` | Get map with streets |
| POST | `/api/maps` | Create map |
| PUT | `/api/maps/:id` | Update map name |
| DELETE | `/api/maps/:id` | Delete map |
| POST | `/api/maps/:id/streets` | Add street to map |
| PUT | `/api/maps/streets/:id` | Update street |
| DELETE | `/api/maps/streets/:id` | Delete street |

### Games
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/games` | List all games |
| GET | `/api/games/:id` | Get game |
| POST | `/api/games` | Create game |
| POST | `/api/games/:id/invite` | Invite player |
| GET | `/api/games/:id/players` | List game players |
| GET | `/api/games/:id/admin-view` | Admin dashboard data |

### Player
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/player/game/:joinToken` | Player's game view |
| POST | `/api/player/game/:joinToken/checkin` | Check in at street |

### Invite
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/invite/:token/accept` | Accept invite (redirects to player app) |

---

## Auth0 Setup (Admin App Security)

The admin app and its API endpoints are secured with Auth0 using OAuth 2.0 / OIDC.

### 1. Create an Auth0 Account

Sign up for a free account at [auth0.com](https://auth0.com). The free plan supports up to 25,000 monthly active users.

### 2. Create an API

In the Auth0 Dashboard under **Applications → APIs**:

1. Click **+ Create API**
2. Set the **Name** to `Street Monopoly API`
3. Set the **Identifier** to `https://streetmonopoly.api` (this is the audience)
4. Leave **Signing Algorithm** as `RS256`
5. Click **Create**

### 3. Create a Single Page Application

In the Auth0 Dashboard under **Applications → Applications**:

1. Click **+ Create Application**
2. Set the **Name** to `Street Monopoly Admin`
3. Select **Single Page Web Applications**
4. Click **Create**
5. Go to the **Settings** tab and configure:
   - **Allowed Callback URLs**: `http://localhost:3000`
   - **Allowed Logout URLs**: `http://localhost:3000`
   - **Allowed Web Origins**: `http://localhost:3000`
6. Note your **Domain** and **Client ID**

### 4. Configure the Backend

Set environment variables (or edit `application.properties`):

```bash
export AUTH0_DOMAIN=your-tenant.us.auth0.com
export AUTH0_AUDIENCE=https://streetmonopoly.api
```

### 5. Configure the Admin App

Create `admin-app/.env`:

```bash
VITE_AUTH0_DOMAIN=your-tenant.us.auth0.com
VITE_AUTH0_CLIENT_ID=your-client-id-from-step-3
VITE_AUTH0_AUDIENCE=https://streetmonopoly.api
```

### Security Model

- **Admin endpoints** (`/api/maps/**`, `/api/games/**`) require a valid Auth0 JWT bearer token. The backend validates the token's signature, issuer, and audience.
- **Player endpoints** (`/api/player/**`) are public — they are secured by the unguessable join token in the URL, so players don't need an Auth0 account.
- **Invite acceptance** (`/api/invite/**`) is public — secured by the unguessable invite token.

---

## Street Colours

The game uses classic Monopoly property group colours:
- 🟤 Brown
- 🔵 Light Blue
- 🩷 Pink
- 🟠 Orange
- 🔴 Red
- 🟡 Yellow
- 🟢 Green
- 🔵 Dark Blue

---

## Key Design Decisions

- **Auth0 + OAuth 2.0** — admin app uses OIDC Authorization Code Flow with PKCE; backend validates JWTs as a resource server
- **MyBatis** for persistence — SQL-first approach with annotation-based mappers
- **UUID primary keys** — portable, no sequence conflicts
- **Token-based player auth** — invite tokens and join tokens (no login required)
- **Haversine formula** — accurate GPS distance calculation for proximity checks
- **Scheduled status updates** — game lifecycle managed by Spring `@Scheduled` (15s interval)
- **Optimistic approach** — balance can go negative; final balance includes unvisited street penalties
- **Email fallback** — if SMTP isn't configured, emails are logged to console for development
