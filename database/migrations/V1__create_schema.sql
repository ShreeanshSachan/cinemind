-- CineMind AI schema. Owned by Flyway inside the Movie Service only.
-- The RAG Service (phase 3) reuses movie_embeddings with spring.flyway.enabled=false.

CREATE TABLE IF NOT EXISTS movies (
    id              BIGINT         NOT NULL AUTO_INCREMENT,
    title           VARCHAR(255)   NOT NULL,
    overview        TEXT           NULL,
    genres          VARCHAR(1000)  NULL,
    release_year    INT            NULL,
    runtime_minutes INT            NULL,
    keywords        TEXT           NULL,
    rating          DECIMAL(3, 1)  NULL,
    poster_url      VARCHAR(512)   NULL,
    created_at      TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT uk_movies_title_year UNIQUE (title, release_year),
    KEY idx_movies_release_year (release_year),
    KEY idx_movies_runtime (runtime_minutes),
    KEY idx_movies_rating (rating)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS movie_embeddings (
    id               BIGINT      NOT NULL AUTO_INCREMENT,
    movie_id         BIGINT      NOT NULL,
    document_content TEXT        NOT NULL,
    -- JSON array of floats serialized as portable text (no vendor vector type).
    embedding        LONGTEXT    NOT NULL,
    model_name       VARCHAR(100) NOT NULL,
    created_at       TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at       TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT uk_movie_embeddings_movie UNIQUE (movie_id),
    CONSTRAINT fk_movie_embeddings_movie FOREIGN KEY (movie_id)
        REFERENCES movies (id) ON DELETE CASCADE,
    KEY idx_movie_embeddings_model (model_name)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;
