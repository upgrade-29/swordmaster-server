package com.swordmaster.common.table.constant;

import com.swordmaster.common.table.GoogleSheetClient;
import com.swordmaster.common.table.SheetTable;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class ConstantTable extends SheetTable<Constant> {
    private final Map<ConstantKey, Object> values = new EnumMap<>(ConstantKey.class);

    public ConstantTable(GoogleSheetClient sheets, @Value("${game-data.gid.config}") long gid) {
        super(sheets, gid, Constant.class);

        Map<String, Object> raw = getRaw();

        for (ConstantKey key : ConstantKey.values())
            values.put(
                    key,
                    key.type().parse(raw.get(key.name()))
            );
    }

    // 검증
    @Override
    protected void validate(List<String> errors) {
        // key는 중복 불가
        checkUnique("key", Constant::key, errors);

        Map<String, Object> raw = getRaw();

        for (ConstantKey key : ConstantKey.values()) {
            Object value = raw.get(key.name());

            if (value == null) {
                errors.add(key + "가 시트에 없거나 값이 비어 있습니다.");

                continue;
            }

            try {
                key.type().parse(value);
            } catch (RuntimeException e) {
                errors.add(key + "의 value(" + value + ")는 " + key.type() + " 타입이 아닙니다.");
            }
        }
    }

    // 검색
    public int    getInt(ConstantKey key)     { return (Integer) require(key, ConstantType.INT); }
    public long   getLong(ConstantKey key)    { return (Long)    require(key, ConstantType.LONG); }
    public double getDecimal(ConstantKey key) { return (Double)  require(key, ConstantType.DECIMAL); }
    public String getText(ConstantKey key)    { return (String)  require(key, ConstantType.TEXT); }

    @SuppressWarnings("unchecked")      // 컴파일러의 '확인되지 않은 형변환' 경고를 끄는 어노테이션
    public List<String> getTextList(ConstantKey key) {
        return (List<String>) require(key, ConstantType.TEXT_LIST);
    }

    // 공통
    private Map<String, Object> getRaw() {
        Map<String, Object> raw = new HashMap<>();

        for (Constant constant : rows)
            if (constant.key() != null)
                raw.put(constant.key(), constant.value());

        return raw;
    }

    private Object require(ConstantKey key, ConstantType expected) {
        if (key.type() != expected) {
            throw new IllegalStateException(key + "의 타입(" + key.type() + ")과 요청한 타입(" + expected + ")이 다릅니다.");
        }

        return values.get(key);
    }
}
