package com.swordmaster.battle.table;

import com.swordmaster.common.table.GoogleSheetClient;
import com.swordmaster.common.table.SheetTable;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class EnemyTable extends SheetTable<Enemy> {
    private final Map<String, Enemy> byCode;

    public EnemyTable(GoogleSheetClient sheets, @Value("${game-data.gid.enemies}") long gid) {
        super(sheets, gid, Enemy.class);    // 변환 후 검증까지 실행

        // validate에서 중복이 걸러진 뒤라 키 충돌 없음
        byCode = rows.stream()
                .collect(Collectors.toUnmodifiableMap(Enemy::code, Function.identity()));
    }

    // 검증
    @Override
    protected void validate(List<String> errors) {
        // code는 Unique
        checkUnique("code", Enemy::code, errors);

        for (int i = 0; i < rows.size(); i++) {
            Enemy enemy = rows.get(i);

            // code는 Not Blank (이후 메시지의 기준이므로 먼저 검사)
            if (isBlank(enemy.code())) {
                errors.add((i + 1) + "행의 code가 비어 있습니다.");

                continue;
            }

            String at = enemy.code() + "의 ";

            // 문자열 컬럼은 Not Blank
            if (isBlank(enemy.name()))           errors.add(at + "name이 비어 있습니다.");
            if (isBlank(enemy.code())) errors.add(at + "appearanceCode가 비어 있습니다."); //해당 부분의 appearanceCode를 code 로 변경

            // 공격속도, 최대 체력은 0보다 커야 함 (공격 시각 = n / 공격속도)
            if (enemy.attack()      <  0) errors.add(at + "attack은 0 이상이어야 합니다.");
            if (enemy.attackSpeed() <= 0) errors.add(at + "attackSpeed는 0보다 커야 합니다.");
            if (enemy.maxHp()       <= 0) errors.add(at + "maxHp는 0보다 커야 합니다.");

            // 비율은 0 이상 1 이하
            if (enemy.critRate()  < 0 || enemy.critRate()  > 1) errors.add(at + "critRate는 0 이상 1 이하여야 합니다.");
            if (enemy.lifesteal() < 0 || enemy.lifesteal() > 1) errors.add(at + "lifesteal은 0 이상 1 이하여야 합니다.");
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    // 검색
    public Optional<Enemy> findByCode(String code) {
        return Optional.ofNullable(byCode.get(code));
    }
}
