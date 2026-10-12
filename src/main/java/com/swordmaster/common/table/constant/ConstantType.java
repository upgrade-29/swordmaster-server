package com.swordmaster.common.table.constant;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.function.Function;

public enum ConstantType {
    INT       (s -> new BigDecimal(s).intValueExact()),     // 소수로 먼저 변환 후 재변환, 소수면 예외 (소숫점 뒤가 0이면 통과)
    LONG      (s -> new BigDecimal(s).longValueExact()),
    DECIMAL   (Double::parseDouble),
    TEXT      (s -> s),
    TEXT_LIST (s -> Arrays.stream(s.split(","))     // 쉼표 구분
            .map(String::trim)                                  // 공백 제거
            .filter(v -> !v.isEmpty())
            .toList());

    private final Function<String, Object> parser;

    ConstantType(Function<String, Object> parser) { this.parser = parser; }

    // Object 형식으로 받은 원본 값을 String 타입으로 변환하고, 공백을 제거한 후 Parser에 적용
    public Object parse(Object raw) {
        return parser.apply(String.valueOf(raw).trim());
    }
}
