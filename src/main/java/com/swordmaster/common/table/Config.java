package com.swordmaster.common.table;

// config 시트의 한 행 (value는 시트 값에 따라 Long / Double / String)
public record Config(
        String key,             // PK
        Object value,
        String description
) {
}
