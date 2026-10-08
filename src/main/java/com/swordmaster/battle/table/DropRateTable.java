package com.swordmaster.battle.table;

import com.swordmaster.common.table.GoogleSheetClient;
import com.swordmaster.common.table.SheetTable;
import com.swordmaster.equipment.EquipmentGrade;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class DropRateTable extends SheetTable<DropRate> {
    private static final double SUM_EPSILON = 1e-9;

    private final List<DropRate> byGrade;

    public DropRateTable(GoogleSheetClient sheets, @Value("${game-data.gid.droprates}") long gid) {
        super(sheets, gid, DropRate.class);     // 변환 후 검증까지 실행

        // 등급 순서(COMMON -> LEGENDARY)로 정렬
        byGrade = rows.stream()
                .sorted(Comparator.comparing(DropRate::grade))
                .toList();
    }

    // 검증
    @Override
    protected void validate(List<String> errors) {
        // grade는 Unique (열거형이 아닌 값은 변환 단계에서 걸러짐)
        checkUnique("grade", DropRate::grade, errors);

        for (DropRate dropRate : rows) {
            // grade는 Not Null
            if (dropRate.grade() == null) {
                errors.add("grade가 비어 있는 행이 있습니다.");

                continue;
            }

            if (dropRate.rate() < 0 || dropRate.rate() > 1)
                errors.add(dropRate.grade() + "의 rate는 0 이상 1 이하여야 합니다.");
        }

        // 모든 등급이 있어야 함
        Set<EquipmentGrade> missing = EnumSet.allOf(EquipmentGrade.class);
        Set<EquipmentGrade> grades  = rows.stream()
                .map(DropRate::grade)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        missing.removeAll(grades);

        if (!missing.isEmpty()) errors.add("grade가 존재하지 않습니다: " + missing);

        // 확률의 합은 1
        double sum = rows.stream().mapToDouble(DropRate::rate).sum();

        if (Math.abs(sum - 1) > SUM_EPSILON) errors.add("rate의 합이 1이 아닙니다: " + sum);
    }

    // 검색
    @Override
    public List<DropRate> getAll() { return byGrade; }
}
