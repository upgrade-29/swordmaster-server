package com.swordmaster.equipment.artifact.table;

import com.swordmaster.common.table.GoogleSheetClient;
import com.swordmaster.common.table.SheetTable;
import com.swordmaster.equipment.EquipmentStat;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class ArtifactStatTable extends SheetTable<ArtifactStat> {
    private record Key(String code, EquipmentStat statType) {}      // 복합 키

    private final List<ArtifactStat>              sorted;       // code -> statType 오름차순
    private final Map<String, List<ArtifactStat>> byCode;       // code 별 조회 (리스트)
    private final Map<Key, ArtifactStat>          byKey;        // Key(복합키) 별 조회 (단일)

    public ArtifactStatTable(
            GoogleSheetClient         sheets,
            @Value("1319205147") long gid,
            ArtifactTable             artifactTable) {
        super(sheets, gid, ArtifactStat.class);

        // Artifact 테이블과의 관계를 추가로 검증
        validateRelation(artifactTable);        // 생성자 단계에서 호출해도 스프링이 알아서 필요한 컴포넌트를 먼저 자동 주입

        sorted = rows.stream()
                .sorted(
                        Comparator.comparing(ArtifactStat::code)
                                .thenComparing(ArtifactStat::statType)
                )
                .toList();

        byCode = Map.copyOf(
                sorted.stream()
                        .collect(
                                Collectors.groupingBy(
                                        ArtifactStat::code,
                                        Collectors.toUnmodifiableList())
                        )
        );

        byKey = rows.stream()
                .collect(
                        Collectors.toUnmodifiableMap(
                                s -> new Key(s.code(), s.statType()),
                                Function.identity()
                        )
                );

    }

    // 검증 - 테이블 기본 검증
    @Override
    protected void validate(List<String> errors) {
        // 복합 키 검증
        checkUnique("code + statType", s -> new Key(s.code(), s.statType()), errors);

        // 각 컬럼들에 대하여
        for (int i = 0; i < rows.size(); i++) {
            ArtifactStat stat = rows.get(i);
            String       at   = (i + 1) + "행(code: " + stat.code() + ")의 ";

            // code는 Not Blank
            if (stat.code() == null || stat.code().isBlank()) errors.add(at + "code가 비어 있습니다.");

            // statType은 Not Null (열거형이 아닌 값(null 제외)은 변환 단계에서 이미 걸러짐)
            if (stat.statType() == null) errors.add(at + "statType이 비어 있습니다.");

            // 기타 컬럼은 각각 0 이상
            if (stat.baseValue()     < 0) errors.add(at + "baseValue는 0 이상이어야 합니다.");
            if (stat.valuePerLevel() < 0) errors.add(at + "valuePerLevel은 0 이상이어야 합니다.");
        }
    }

    // 검증 (추가) - Artifact 테이블과의 관계
    private void validateRelation(ArtifactTable artifactTable) {
        List<String> errors = new ArrayList<>();

        // 스텟의 code는 Artifact 테이블에 존재해야 함
        for (int i = 0; i < rows.size(); i++) {
            ArtifactStat stat = rows.get(i);

            if (!artifactTable.existsByCode(stat.code()))
                errors.add((i + 1) + "행의 code(" + stat.code() + ")가 artifacts 테이블에 존재하지 않습니다.");
        }

        // 모든 아티팩트는 1개 이상의 스텟을 가져야 함
        Set<String> statCodes = rows.stream()
                .map(ArtifactStat::code)
                .collect(Collectors.toSet());

        for (Artifact artifact : artifactTable.getAll())
            if (!statCodes.contains(artifact.code()))
                errors.add(artifact.code() + "의 스텟이 존재하지 않습니다.");

        throwIfErrors(errors);
    }

    // 검색
    @Override
    public List<ArtifactStat> getAll() { return sorted; }

    // 검색 - 특정 아티팩트의 스텟 목록 (리스트)
    public List<ArtifactStat> findByCode(String code) {
        return code == null ? List.of() : byCode.getOrDefault(code, List.of());
    }

    // 검색 - 특정 아티팩트의 특정 스텟 (단일)
    public Optional<ArtifactStat> find(String code, EquipmentStat statType) {
        if (code == null || statType == null) return Optional.empty();

        return Optional.ofNullable(byKey.get(new Key(code, statType)));
    }
}
