package com.swordmaster;

import com.swordmaster.common.table.constant.ConstantKey;
import com.swordmaster.common.table.constant.ConstantTable;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class ConstantTableTest {

    @Autowired
    private ConstantTable constantTable;    // 주입되는 시점에 시트 읽기 + 검증 + 변환 완료

    @Test
    void printAll() {
        for (ConstantKey key : ConstantKey.values()) {
            Object value = switch (key.type()) {
                case INT       -> constantTable.getInt(key);
                case LONG      -> constantTable.getLong(key);
                case DECIMAL   -> constantTable.getDecimal(key);
                case TEXT      -> constantTable.getText(key);
                case TEXT_LIST -> constantTable.getTextList(key);
            };

            System.out.printf("%-26s %-10s %s%n", key, key.type(), value);
        }
    }
}
