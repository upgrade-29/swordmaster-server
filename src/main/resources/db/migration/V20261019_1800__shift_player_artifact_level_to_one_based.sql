-- PlayerArtifact 초기 레벨 0 -> 1 변경에 따라 기존 행의 레벨을 1씩 올림
-- 강화 횟수는 그대로 유지 (기존 0레벨 = 새 1레벨)
-- 레벨 1 코드와 같은 배포로 적용해야 함 (이미 1부터 저장된 행이 있으면 2가 됨)
UPDATE player_artifacts
    SET level = level + 1;
