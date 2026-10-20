CREATE TABLE user_identities (
                                 id BIGINT NOT NULL AUTO_INCREMENT,
                                 user_id BIGINT NOT NULL,

                                 provider ENUM('LOCAL', 'GOOGLE', 'KAKAO', 'NAVER') NOT NULL,
                                 provider_user_id VARCHAR(255) NOT NULL,

                                 password_hash VARCHAR(255) NULL,
                                 email VARCHAR(254) NULL,

                                 PRIMARY KEY (id),

                                 UNIQUE KEY uk_identity_provider_subject (
                                     provider,
                                     provider_user_id
                                     ),

                                 CONSTRAINT fk_user_identities_user
                                     FOREIGN KEY (user_id)
                                         REFERENCES users (id)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci;