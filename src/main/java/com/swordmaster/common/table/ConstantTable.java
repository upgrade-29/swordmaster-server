package com.swordmaster.common.table;

import com.swordmaster.common.BusinessException;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public abstract class ConstantTable extends SheetTable<Constant> {
    // 키(name), 값(type, value) 형식의 데이터로 가공
    private final Map<String, Constant.Value> values;

    protected ConstantTable(GoogleSheetClient sheets, long gid) {
        super(sheets, gid, Constant.class);

        Map<String, Constant.Value> map = new LinkedHashMap<>();

        for (Constant constant : rows) map.put(constant.key(), constant.toValue());

        values = Collections.unmodifiableMap(map);
    }

    // 검증
    // 검증은 모든 ConstantTable을 상속하는 클래스에서 동일하게 사용하므로 재정의 금지 (final)
    @Override
    protected final void validate(List<String> errors) {
        // key는 중복 불가
        checkUnique("key", Constant::key, errors);

        for (Constant constant : rows) {
            // key는 Not Null
            if (constant.key() == null || constant.key().isBlank()) {
                errors.add("key가 비어 있는 행이 있습니다.");

                continue;
            }

            // 나머지 컬럼(type, value)에 대하여
            String at = constant.key() + "의 ";

            // 각 컬럼은 Not Null + value의 type이 일치하는지 검사
            if      (constant.type()  == null) errors.add(at + "type이 비어 있습니다.");
            else if (constant.value() == null) errors.add(at + "value가 비어 있습니다.");
            else if (!constant.type().matches(constant.value())) {
                errors.add(at + "value가 " + constant.type() + " 타입이 아닙니다.");
            }
        }
    }

    // 검색
    // 부모 클래스의 getAll() 사용 방지
    @Deprecated
    @Override
    public final List<Constant> getAll() {
        throw new BusinessException(getTableName() + ": 상수 테이블은 getAllValues()를 사용하세요.");
    }

    // 추가 가공된 데이터 형식으로 반환
    public Map<String, Constant.Value> getAllValues() { return values; }

    // 키를 통한 검색
    public int getInt(String key) {
        return Math.toIntExact(getLong(key));
    }

    public long getLong(String key) {
        return ((Number) require(key, Constant.Type.INTEGER)).longValue();
    }

    public double getDouble(String key) {
        return ((Number) require(key, Constant.Type.DECIMAL)).doubleValue();
    }

    public String getText(String key) {
        return String.valueOf(require(key, Constant.Type.TEXT));
    }

    // value(Object)를 검사 후 반환
    // 예외 처리 방식은 수정 필요 (클라이언트가 아닌 서버 코드 이상)
    private Object require(String key, Constant.Type type) {
        Constant.Value found = values.get(key);

        // key가 존재하지 않으면 에러
        if (found == null) {
            throw new BusinessException(getTableName() + ": " + key + " key가 존재하지 않습니다.");
        }

        // 요구하는 value의 type이 key의 type과 일치하지 않으면
        if (found.type() != type) {
            throw new BusinessException(
                    getTableName() + ": " + key + " key의 type은 " + type + "이 아닙니다."
            );
        }

        return found.value();
    }
}
