-- Initial schema baseline.
-- spring.flyway.baseline-on-migrate=true means this file is treated as
-- "already applied" on an existing database so nothing runs destructively.

CREATE TABLE IF NOT EXISTS users (
    id            VARCHAR(255)  PRIMARY KEY,
    email         VARCHAR(255)  UNIQUE,
    phone_number  VARCHAR(255)  UNIQUE,
    display_name  VARCHAR(50),
    password_hash VARCHAR(255)  NOT NULL,
    role          VARCHAR(20),
    created_at    TIMESTAMP     NOT NULL
);

CREATE TABLE IF NOT EXISTS visits (
    id           BIGSERIAL     PRIMARY KEY,
    user_id      VARCHAR(255)  NOT NULL,
    place_id     VARCHAR(255)  NOT NULL,
    name         VARCHAR(255)  NOT NULL,
    address      VARCHAR(255)  NOT NULL,
    latitude     DOUBLE PRECISION,
    longitude    DOUBLE PRECISION,
    rating       DOUBLE PRECISION,
    country_code VARCHAR(10),
    continent    VARCHAR(4),
    irish_county VARCHAR(64),
    us_state     VARCHAR(64),
    city         VARCHAR(128),
    maps_url     VARCHAR(1024),
    photo_url    VARCHAR(1024),
    created_at   TIMESTAMP     NOT NULL,
    CONSTRAINT uq_visits_user_place UNIQUE (user_id, place_id)
);

CREATE TABLE IF NOT EXISTS favourites (
    id           BIGSERIAL     PRIMARY KEY,
    user_id      VARCHAR(255)  NOT NULL,
    place_id     VARCHAR(255)  NOT NULL,
    name         VARCHAR(255)  NOT NULL,
    address      VARCHAR(255)  NOT NULL,
    latitude     DOUBLE PRECISION,
    longitude    DOUBLE PRECISION,
    rating       DOUBLE PRECISION,
    maps_url     VARCHAR(1024),
    photo_url    VARCHAR(1024),
    created_at   TIMESTAMP     NOT NULL,
    CONSTRAINT uq_favourites_user_place UNIQUE (user_id, place_id)
);

CREATE TABLE IF NOT EXISTS friendships (
    id           BIGSERIAL     PRIMARY KEY,
    requester_id VARCHAR(255)  NOT NULL,
    addressee_id VARCHAR(255)  NOT NULL,
    status       VARCHAR(20)   NOT NULL,
    created_at   TIMESTAMP     NOT NULL,
    CONSTRAINT uq_friendships UNIQUE (requester_id, addressee_id)
);

CREATE TABLE IF NOT EXISTS guinness_reviews (
    id          BIGSERIAL    PRIMARY KEY,
    user_id     VARCHAR(255) NOT NULL,
    place_id    VARCHAR(255) NOT NULL,
    creaminess  INTEGER      NOT NULL,
    temperature INTEGER      NOT NULL,
    quality     INTEGER      NOT NULL,
    price       INTEGER      NOT NULL,
    overall     INTEGER      NOT NULL,
    created_at  TIMESTAMP    NOT NULL,
    updated_at  TIMESTAMP,
    CONSTRAINT uq_guinness_reviews_user_place UNIQUE (user_id, place_id)
);

CREATE TABLE IF NOT EXISTS badge_events (
    id                BIGSERIAL     PRIMARY KEY,
    user_id           VARCHAR(255)  NOT NULL,
    badge_id          VARCHAR(255)  NOT NULL,
    badge_name        VARCHAR(255),
    badge_description VARCHAR(500),
    badge_icon        VARCHAR(255),
    badge_color       VARCHAR(12),
    badge_category    VARCHAR(255),
    earned_at         TIMESTAMP     NOT NULL,
    CONSTRAINT uq_badge_events_user_badge UNIQUE (user_id, badge_id)
);

CREATE TABLE IF NOT EXISTS place_search_cache (
    id            BIGSERIAL    PRIMARY KEY,
    cell_key      VARCHAR(255) NOT NULL,
    response_json TEXT         NOT NULL,
    expires_at    TIMESTAMPTZ  NOT NULL,
    created_at    TIMESTAMPTZ  NOT NULL
);

CREATE TABLE IF NOT EXISTS place_details_cache (
    id            BIGSERIAL    PRIMARY KEY,
    place_id      VARCHAR(255) NOT NULL UNIQUE,
    response_json TEXT         NOT NULL,
    expires_at    TIMESTAMPTZ  NOT NULL,
    created_at    TIMESTAMPTZ  NOT NULL
);

CREATE TABLE IF NOT EXISTS api_call_logs (
    id        BIGSERIAL   PRIMARY KEY,
    call_type VARCHAR(40) NOT NULL,
    user_id   VARCHAR(255),
    called_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_api_call_logs_called_at ON api_call_logs (called_at);
CREATE INDEX IF NOT EXISTS idx_api_call_logs_user_id   ON api_call_logs (user_id);
