package com.swordmaster.common.table;

import com.swordmaster.common.table.constant.ConstantKey;
import com.swordmaster.common.table.constant.ConstantTable;
import org.springframework.stereotype.Component;

// 서버가 쓰는 게임 데이터 버전 (클라 GameDB.version과 같아야 같은 데이터로 계산함)
// config 시트의 X_GAME_DATA_VERSION 값을 사용 (키가 없으면 ConstantTable 검증에서 부팅 실패)
@Component
public class GameDataVersion {
    private final String version;

    public GameDataVersion(ConstantTable constantTable) {
        version = constantTable.getText(ConstantKey.X_GAME_DATA_VERSION);
    }

    public boolean matches(String clientVersion) {
        return version.equals(clientVersion);
    }
}
