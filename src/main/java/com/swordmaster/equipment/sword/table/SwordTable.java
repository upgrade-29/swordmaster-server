package com.swordmaster.equipment.sword.table;

import com.swordmaster.common.table.GoogleSheetClient;
import com.swordmaster.common.table.SheetTable;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class SwordTable extends SheetTable<Sword> {
    private final List<Sword> sortedByLevel;

    public SwordTable(GoogleSheetClient sheets, @Value("${game-data.gid.swords}") long gid) {
        super(sheets, gid, Sword.class);    // 변환 후 검증까지 실행

        // level의 오름차순 정렬
        sortedByLevel = rows.stream()
                .sorted(Comparator.comparingInt(Sword::level))
                .toList();
    }

    // 검증
    @Override
    protected void validate(List<String> errors) {
        // level, name은 Unique
        checkUnique("level", Sword::level, errors);
        checkUnique(
                "name",
                sword -> sword.name() == null ? null : sword.name().strip(),
                errors
        );

        // level은 0부터 swords 갯수 - 1까지 모든 숫자가 빠짐없이 있어야 함 (Not Blank)
        Set<Integer> levels = rows.stream()
                .map(Sword::level)
                .collect(Collectors.toSet());

        for (int level = 0; level < rows.size(); level++)
            if (!levels.contains(level))
                errors.add("level이 존재하지 않습니다: " + level);

        // 이후 나머지 컬럼들에 대하여
        for (Sword sword : rows) {
            String at = "level(" + sword.level() + ")의 ";

            // name은 Not Blank
            if (sword.name() == null || sword.name().isBlank())
                errors.add(at + "name이 비어 있습니다.");

            // successRate는 0 이상 1 이하 (Null 허용)
            var rate = sword.successRate();

            if (rate != null && (rate < 0 || rate > 1))
                errors.add(at + "successRate는 0 이상 1 이하여야 합니다. (Null 사용 가능)");

            // enhanceCost는 0 이상 (Null 허용)
            var cost = sword.enhanceCost();

            if (cost != null && cost < 0)
                errors.add(at + "enhanceCost는 0 이상이어야 합니다. (Null 사용 가능)");

            // 기타 컬럼은 각각 0 이상
            if (sword.sellPrice()   < 0) errors.add(at + "sellPrice는 0 이상이어야 합니다.");
            if (sword.attackPower() < 0) errors.add(at + "attackPower는 0 이상이어야 합니다.");
            if (sword.attackSpeed() < 0) errors.add(at + "attackSpeed는 0 이상이어야 합니다.");
            if (sword.maxHp()       < 0) errors.add(at + "maxHp는 0 이상이어야 합니다.");

            // appearanceCode는 Not Blank
            // name은 Not Blank
            if (sword.appearanceCode() == null || sword.appearanceCode().isBlank())
                errors.add(at + "name이 비어 있습니다.");
        }
    }

    // 검색
    @Override
    public List<Sword> getAll() { return sortedByLevel; }

    public Sword getFirst() { return sortedByLevel.getFirst(); }

    public Optional<Sword> findByLevel(int level) {
        if (level < 0 || level >= sortedByLevel.size()) return Optional.empty();

        return Optional.of(sortedByLevel.get(level));
    }
}
