package com.swordmaster.equipment.artifact.table;

import com.swordmaster.common.table.GoogleSheetClient;
import com.swordmaster.common.table.SheetTable;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;

@Component
public class ArtifactTable extends SheetTable<Artifact> {
    private final List<Artifact> byCode;

    public ArtifactTable(GoogleSheetClient sheets, @Value("${game-data.gid.artifacts}") long gid) {
        super(sheets, gid, Artifact.class);     // 변환 후 검증까지 실행

        // code의 오름차순 정렬
        byCode = rows.stream()
                .sorted(Comparator.comparing(Artifact::code))
                .toList();
    }

    // 검증
    @Override
    protected void validate(List<String> errors) {
        // code는 Unique
        checkUnique("code", Artifact::code, errors);

        // 각 컬럼들에 대하여
        for (int i = 0; i < rows.size(); i++) {
            Artifact artifact = rows.get(i);
            String at = (i + 1) + "행의 ";

            // code는 Not Blank
            if (artifact.code() == null || artifact.code().isBlank())
                errors.add(at + "code가 비어 있습니다.");

            // name은 Not Blank
            if (artifact.name() == null || artifact.name().isBlank())
                errors.add(at + "name이 비어 있습니다.");

            // grade, statType은 Not Null (열거형이 아닌 값은 변환 단계에서 걸러짐)
            if (artifact.grade()    == null) errors.add(at  + "grade가 비어 있습니다.");
            if (artifact.statType() == null) errors.add((at + "statType이 비어 있습니다."));

            // 기타 컬럼은 각각 0 이상
            if (artifact.baseValue()     < 0) errors.add(at + "baseValue는 0 이상이어야 합니다.");
            if (artifact.valuePerLevel() < 0) errors.add(at + "valuePerLevel은 0 이상이어야 합니다.");
        }
    }

    // 검색
    @Override
    public List<Artifact> getAll() { return byCode; }

//    // 열거형의 제한과 그에 따른 모든 조합이 존재한다는 것이 검증되므로 항상 값이 존재
//    public Artifact findByKey(EquipmentStat stat, EquipmentRarity rarity) {
//        return byKey.get(new Key(stat, rarity));
//    }
}
