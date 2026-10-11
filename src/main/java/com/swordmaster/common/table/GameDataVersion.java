package com.swordmaster.common.table;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

// 서버가 쓰는 게임 데이터 버전 (클라 GameDB.version과 같아야 같은 데이터로 계산함)
// config 시트의 GAME_DATA_VERSION 값을 사용하고, 키가 없으면 버전 검증을 하지 않음
@Slf4j
@Component
public class GameDataVersion {
    private static final String KEY = "GAME_DATA_VERSION";

    private final String version;   // Null 허용 (null 이면 검증 생략)

    public GameDataVersion(ConfigTable configTable) {
        version = configTable.contains(KEY) ? configTable.getText(KEY) : null;

        if (version == null) log.warn("config 시트에 {}가 없어 게임 데이터 버전을 검증하지 않습니다.", KEY);
    }

    public boolean matches(String clientVersion) {
        return version == null || version.equals(clientVersion);
    }
}
