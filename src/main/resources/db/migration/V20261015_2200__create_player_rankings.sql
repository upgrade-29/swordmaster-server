CREATE TABLE player_rankings (
                                 id          BIGINT      NOT NULL AUTO_INCREMENT,
                                 player_id   BIGINT      NOT NULL,
                                 type        VARCHAR(20) NOT NULL,
                                 score       BIGINT      NOT NULL,
                                 achieved_at DATETIME(6) NOT NULL,
                                 PRIMARY KEY (id),
                                 UNIQUE KEY uk_player_rankings_player_type (player_id, type),
                                 CONSTRAINT fk_player_rankings_player FOREIGN KEY (player_id) REFERENCES players (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;