-- PlayerArtifact @Version 추가 (낙관적 락)
-- 기존 행은 0부터 시작
ALTER TABLE player_artifacts
    ADD COLUMN version BIGINT NOT NULL DEFAULT 0;