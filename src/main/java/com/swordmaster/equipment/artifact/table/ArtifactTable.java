package com.swordmaster.equipment.artifact.table;

import com.swordmaster.common.table.GoogleSheetClient;
import com.swordmaster.common.table.SheetTable;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class ArtifactTable extends SheetTable<Artifact> {
    private final List<Artifact>        sorted;
    private final Map<String, Artifact> byCode;

    public ArtifactTable(GoogleSheetClient sheets, @Value("${game-data.gid.artifacts}") long gid) {
        super(sheets, gid, Artifact.class);     // 변환 후 검증까지 실행

        sorted = rows.stream()
                .sorted(Comparator.comparing(Artifact::code))
                .toList();

        byCode = rows.stream()
                .collect(Collectors.toUnmodifiableMap(Artifact::code, Function.identity()));
    }

    // 검증
    @Override
    protected void validate(List<String> errors) {
        // code는 Unique
        checkUnique("code", Artifact::code, errors);

        // 각 컬럼들에 대하여
        for (int i = 0; i < rows.size(); i++) {
            Artifact artifact = rows.get(i);
            String   at       = (i + 1) + "행의 ";

            // code는 Not Blank
            if (artifact.code() == null || artifact.code().isBlank()) errors.add(at + "code가 비어 있습니다.");

            // name은 Not Blank
            if (artifact.name() == null || artifact.name().isBlank()) errors.add(at + "name이 비어 있습니다.");

            // grade는 Not Null (열거형이 아닌 값(null 제외)은 변환 단계에서 걸러짐)
            if (artifact.grade() == null) errors.add(at + "grade가 비어 있습니다.");
        }
    }

    // 검색
    @Override
    public List<Artifact> getAll() { return sorted; }

    public boolean existsByCode(String code) { return byCode.containsKey(code); }

    public Optional<Artifact> findByCode(String code) {
        return code == null ? Optional.empty() : Optional.ofNullable(byCode.get(code));
    }
}
