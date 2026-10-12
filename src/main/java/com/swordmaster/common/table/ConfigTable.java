package com.swordmaster.common.table;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

// 게임 전체에서 쓰는 설정값 (key, value, description)
// 키가 없거나 타입이 다르면 서버 코드 / 시트 데이터 오류이므로 IllegalStateException
@Component
public class ConfigTable extends SheetTable<Config> {
    private final Map<String, Object> byKey;

    public ConfigTable(GoogleSheetClient sheets, @Value("${game-data.gid.config}") long gid) {
        super(sheets, gid, Config.class);   // 변환 후 검증까지 실행

        // validate에서 중복과 빈 값이 걸러진 뒤라 키 충돌, null 없음
        byKey = rows.stream()
                .collect(Collectors.toUnmodifiableMap(Config::key, Config::value));
    }

    // 검증
    @Override
    protected void validate(List<String> errors) {
        // key는 Unique
        checkUnique("key", Config::key, errors);

        for (Config config : rows) {
            // key는 Not Blank
            if (config.key() == null || config.key().isBlank()) {
                errors.add("key가 비어 있는 행이 있습니다.");

                continue;
            }

            // value는 Not Null
            if (config.value() == null) errors.add(config.key() + "의 value가 비어 있습니다.");
        }
    }

    // 검색
    public boolean contains(String key) { return byKey.containsKey(key); }

    // 정수 칸(1)도 실수로 읽을 수 있음
    public double getDouble(String key) {
        return require(key, Number.class, "숫자").doubleValue();
    }

    public long getLong(String key) {
        return require(key, Long.class, "정수");
    }

    public String getText(String key) {
        return String.valueOf(require(key, Object.class, "값"));
    }

    private <V> V require(String key, Class<V> type, String typeName) {
        Object value = byKey.get(key);

        if (value == null) throw new IllegalStateException(getTableName() + ": " + key + " 키가 존재하지 않습니다.");

        if (!type.isInstance(value)) {
            throw new IllegalStateException(getTableName() + ": " + key + "의 값이 " + typeName + "가 아닙니다: " + value);
        }

        return type.cast(value);
    }
}
