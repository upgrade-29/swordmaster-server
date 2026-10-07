package com.swordmaster.common.table;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.util.*;
import java.util.function.Function;

public abstract class SheetTable<T> {
    private static final ObjectMapper MAPPER = JsonMapper.builder()
            .enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)          // 존재하지 않는 컬럼이 오면 에러
            .enable(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES)         // 기본형(int 등) 필드에 null이 오면 에러
            .enable(DeserializationFeature.FAIL_ON_MISSING_CREATOR_PROPERTIES)  // 컬럼 자체가 없으면 에러
            .disable(DeserializationFeature.ACCEPT_FLOAT_AS_INT)                // 정수 칸에 소수가 들어오면 에러
            .build();

    protected final List<T> rows;

    protected SheetTable(GoogleSheetClient sheets, long gid, Class<T> type) {
        List<Map<String, Object>> raw       = sheets.readSheet(gid);
        List<T>                   converted = new ArrayList<>();
        List<String>              errors    = new ArrayList<>();

        // 행마다 변환 후 1차 검증
        for (int i = 0; i < raw.size(); i++) {
            try {
                converted.add(MAPPER.convertValue(raw.get(i), type));
            } catch (JacksonException e) {
                errors.add("데이터 " + (i + 1) + "번째 행: " + e.getOriginalMessage());
            }
        }

        throwIfErrors(errors);

        if (converted.isEmpty()) throw new IllegalStateException(getTableName() + "에 데이터가 없습니다.");

        rows = List.copyOf(converted);      // 변환 결과

        // 테이블 별 검증
        validate(errors);
        throwIfErrors(errors);
    }

    // 검증
    protected abstract void validate(List<String> errors);      // 추상 메서드

    protected <K> void checkUnique(String label, Function<T, K> key, List<String> errors) {
        Set<K> seen = new HashSet<>();

        for (T row : rows) {
            K value = key.apply(row);

            if (value != null && !seen.add(value)) errors.add(label + "이 중복됩니다: " + value);
        }
    }

    protected void throwIfErrors(List<String> errors) {
        if (errors.isEmpty()) return;

        throw new IllegalStateException(
                getTableName() + " 검증에 실패하였습니다:\n - " + String.join("\n - ", errors)
        );
    }

    // 검색
    protected final String getTableName() { return getClass().getSimpleName(); }

    public List<T> getAll() { return rows; }
}
