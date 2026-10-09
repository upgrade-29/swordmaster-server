package com.swordmaster.equipment.artifact.table;

import com.swordmaster.common.table.GoogleSheetClient;
import com.swordmaster.common.table.SheetTable;
import com.swordmaster.equipment.EquipmentGrade;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class ArtifactEnhanceTable extends SheetTable<ArtifactEnhance> {
    private record Key(EquipmentGrade grade, int level) {}      // 복합 키

    private final List<ArtifactEnhance>     sorted;
    private final Map<Key, ArtifactEnhance> byKey;

    public ArtifactEnhanceTable(GoogleSheetClient sheets, @Value("${game-data.gid.artifact-enhance}") long gid) {
        super(sheets, gid, ArtifactEnhance.class);

        sorted = rows.stream()
                .sorted(Comparator.comparing(ArtifactEnhance::grade).thenComparingInt(ArtifactEnhance::level))
                .toList();

        byKey = rows.stream()
                .collect(Collectors.toUnmodifiableMap(enhance -> new Key(enhance.grade(), enhance.level()), Function.identity()));
    }

    @Override
    protected void validate(List<String> errors) {
        // grade + level 조합은 Unique
        checkUnique("grade + level", enhance -> new Key(enhance.grade(), enhance.level()), errors);

        // 각 컬럼들에 대하여
        for (int i = 0; i < rows.size(); i++) {
            ArtifactEnhance enhance = rows.get(i);
            String          at      = (i + 1) + "행의 ";

            // grade는 Not Null
            if (enhance.grade() == null) errors.add(at + "grade가 비어 있습니다.");

            // 기타 컬럼은 0 이상
            if (enhance.level()         < 0) errors.add(at + "level은 0 이상이어야 합니다.");
            if (enhance.materialCount() < 0) errors.add(at + "materialCount는 0 이상이어야 합니다.");
            if (enhance.gold()          < 0) errors.add(at + "gold는 0 이상이어야 합니다.");
        }
    }

    @Override
    public List<ArtifactEnhance> getAll() { return sorted; }

    public Optional<ArtifactEnhance> findByGradeAndLevel(EquipmentGrade grade, int level) {
        return Optional.ofNullable(byKey.get(new Key(grade, level)));
    }
}
