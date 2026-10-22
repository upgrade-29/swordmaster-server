-- 게스트는 이메일 없이 생성한다.
ALTER TABLE users
    MODIFY COLUMN email VARCHAR(254) NULL;

-- Java enum 값을 문자열로 저장한다.
-- GUEST를 저장하고 이후 제공자 확장도 수용한다.
ALTER TABLE user_identities
    MODIFY COLUMN provider VARCHAR(20) NOT NULL,
    MODIFY COLUMN provider_user_id VARCHAR(255)
        CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL;

-- 한 게임 계정에는 제공자별 계정을 하나만 연결한다.
ALTER TABLE user_identities
    ADD CONSTRAINT uk_identity_user_provider
        UNIQUE (user_id, provider);
