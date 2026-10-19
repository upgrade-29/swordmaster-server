package com.swordmaster.battle.table;

import com.swordmaster.common.table.GoogleSheetClient;
import com.swordmaster.common.table.SheetTable;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class StageTable extends SheetTable<Stage> {
    private final List<Stage> byStage;

    public StageTable(GoogleSheetClient sheets, EnemyTable enemyTable, @Value("${game-data.gid.stages}") long gid) {
        super(sheets, gid, Stage.class);    // 변환 후 검증까지 실행

        // 적 코드 검증 (validate는 부모 생성자에서 실행되어 enemyTable을 쓸 수 없으므로 여기서 검증)
        List<String> errors = new ArrayList<>();

        validateEnemies(enemyTable, errors);
        throwIfErrors(errors);

        // stage의 오름차순 정렬
        byStage = rows.stream()
                .sorted(Comparator.comparingInt(Stage::stage))
                .toList();
    }

    // 검증
    @Override
    protected void validate(List<String> errors) {
        // stage는 Unique
        checkUnique("stage", Stage::stage, errors);

        // stage는 1부터 스테이지 갯수까지 모든 숫자가 빠짐없이 있어야 함
        Set<Integer> stages = rows.stream()
                .map(Stage::stage)
                .collect(Collectors.toSet());

        for (int stage = 1; stage <= rows.size(); stage++)
            if (!stages.contains(stage))
                errors.add("stage가 존재하지 않습니다: " + stage);

        // 이후 나머지 컬럼들에 대하여
        for (Stage stage : rows) {
            String at = "stage(" + stage.stage() + ")의 ";

            if (stage.enemyCount()       <  0) errors.add(at + "enemyCount는 0 이상이어야 합니다.");
            if (stage.statMultiplier()   <= 0) errors.add(at + "statMultiplier는 0보다 커야 합니다.");
            if (stage.goldPerEnemy()     <  0) errors.add(at + "goldPerEnemy는 0 이상이어야 합니다.");
            if (stage.bossGold()         <  0) errors.add(at + "bossGold는 0 이상이어야 합니다.");
            if (stage.recommendedPower() <  0) errors.add(at + "recommendedPower는 0 이상이어야 합니다.");

            if (stage.artifactDropRate() < 0 || stage.artifactDropRate() > 1)
                errors.add(at + "artifactDropRate는 0 이상 1 이하여야 합니다.");

            if (isBlank(stage.imageCode())) errors.add(at + "imageCode가 비어 있습니다.");
            if (isBlank(stage.name()))      errors.add(at + "name이 비어 있습니다.");
        }
    }

    // 일반 적 코드는 일반 적, 보스 코드는 보스여야 함
    private void validateEnemies(EnemyTable enemyTable, List<String> errors) {
        for (Stage stage : rows) {
            String at = "stage(" + stage.stage() + ")의 ";

            checkEnemy(enemyTable, stage.enemyCode(), false, at + "enemyCode", errors);
            checkEnemy(enemyTable, stage.bossCode(),  true,  at + "bossCode",  errors);
        }
    }

    private static void checkEnemy(EnemyTable enemyTable, String code, boolean boss, String label, List<String> errors) {
        if (isBlank(code)) {
            errors.add(label + "가 비어 있습니다.");

            return;
        }

        Optional<Enemy> enemy = enemyTable.findByCode(code);

        if      (enemy.isEmpty())            errors.add(label + "가 EnemyTable에 없습니다: " + code);
        else if (enemy.get().boss() != boss) errors.add(label + "의 boss 값이 " + !boss + "입니다: " + code);
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    // 검색
    @Override
    public List<Stage> getAll() { return byStage; }

    public Optional<Stage> findByStage(int stage) {
        if (stage < 1 || stage > byStage.size()) return Optional.empty();

        return Optional.of(byStage.get(stage - 1));
    }
}
