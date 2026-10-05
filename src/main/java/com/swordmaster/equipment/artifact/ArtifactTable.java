package com.swordmaster.equipment.artifact;

import com.swordmaster.common.table.GoogleSheetClient;
import com.swordmaster.common.table.SheetTable;
import com.swordmaster.equipment.EquipmentRarity;
import com.swordmaster.equipment.EquipmentStat;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class ArtifactTable extends SheetTable<Artifact> {
    // 복합 키 (stat + rarity)
    private record Key(EquipmentStat stat, EquipmentRarity rarity) {}

    private final List<Artifact>     sorted;
    private final Map<Key, Artifact> byKey;

    public ArtifactTable(GoogleSheetClient sheets, @Value("${game-data.gid.artifacts}") long gid) {
        super(sheets, gid, Artifact.class);     // 변환 후 검증까지 실행

        // stat + rarity 오름차순 정렬
        sorted = rows.stream()
                .sorted(Comparator.comparing(Artifact::stat).thenComparing(Artifact::rarity))
                .toList();

        // 키 조회용
        byKey = rows.stream()
                .collect(Collectors.toUnmodifiableMap(
                        artifact -> new Key(artifact.stat(), artifact.rarity()),
                        Function.identity()
                ));
    }

    // 검증
    @Override
    protected void validate(List<String> errors) {
        // 복합 키(stat + rarity) 체크
        //  - 각 조합은 중복 불가
        checkUnique(
                "stat + rarity",
                artifact -> new Key(artifact.stat(), artifact.rarity()),
                errors
        );

        //  - 모든 조합이 존재해야 함
        var keys = rows.stream()
                .map(artifact -> new Key(artifact.stat(), artifact.rarity()))
                .collect(Collectors.toSet());

        for (EquipmentStat stat : EquipmentStat.values())
            for (EquipmentRarity rarity : EquipmentRarity.values())
                if (!keys.contains(new Key(stat, rarity)))
                    errors.add(stat + " / " + rarity + " 조합이 존재하지 않습니다.");

        // 이후 나머지 컬럼들에 대하여
        for (Artifact artifact : rows) {
            String at = artifact.stat() + " / " + artifact.rarity() + "의 ";

            // name은 Not Blank
            if (artifact.name() == null || artifact.name().isBlank())
                errors.add(at + "name이 비어 있습니다.");

            // 기타 컬럼은 각각 0 이상
            if (artifact.baseValue()     < 0) errors.add(at + "baseValue는 0 이상이어야 합니다.");
            if (artifact.valuePerLevel() < 0) errors.add(at + "valuePerLevel은 0 이상이어야 합니다.");
        }
    }

    // 검색
    @Override
    public List<Artifact> getAll() { return sorted; }

    // 열거형의 제한과 그에 따른 모든 조합이 존재한다는 것이 검증되므로 항상 값이 존재
    public Artifact findByKey(EquipmentStat stat, EquipmentRarity rarity) {
        return byKey.get(new Key(stat, rarity));
    }
}
