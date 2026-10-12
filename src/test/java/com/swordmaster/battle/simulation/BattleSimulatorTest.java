package com.swordmaster.battle.simulation;

import com.swordmaster.battle.dto.CombatStat;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.*;

class BattleSimulatorTest {

    // substage-golden.csv: 클라이언트 C# 코드(BattleSubStage.RunToEnd)를 그대로 빌드해서 뽑은 결과 500건
    // double 결과는 비트(Double.doubleToLongBits)로 저장되어 있어 완전히 같은 값인지 비교
    @Test
    void matchesClientGoldenData() throws IOException {
        List<String> failures = new ArrayList<>();
        int          cases    = 0;

        try (InputStream    in     = Objects.requireNonNull(getClass().getResourceAsStream("/battle/substage-golden.csv"));
             BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            String line;

            while ((line = reader.readLine()) != null) {
                if (line.isBlank() || line.startsWith("#")) continue;

                String[] c = line.split(",");
                cases++;

                long    seed          = Long.parseUnsignedLong(c[0]);
                int     subStageIndex = Integer.parseInt(c[1]);
                double  multiplier    = Double.parseDouble(c[3]);
                boolean boss          = c[4].equals("1");
                double  timeLimit     = Double.parseDouble(boss ? c[8] : c[7]);

                CombatStat player = new CombatStat(d(c[9]), d(c[10]), d(c[11]), d(c[12]), d(c[13]));
                double     startHp = d(c[14]);
                CombatStat enemy   = BattleSimulator.enemyStat(
                        new CombatStat(d(c[15]), d(c[16]), d(c[17]), d(c[18]), d(c[19])), multiplier
                );

                SubStageResult actual = new BattleSimulator(d(c[5]), d(c[6]))
                        .simulate(player, startHp, enemy, timeLimit, seed, subStageIndex);

                SubStageResult expected = new SubStageResult(
                        BattleSubStageResultEnum.valueOf(c[20]),
                        bits(c[21]), bits(c[22]), bits(c[23]),
                        Integer.parseInt(c[24]), Integer.parseInt(c[25]),
                        bits(c[26])
                );

                if (!sameBits(expected, actual)) failures.add("line " + cases + "\n  expected=" + expected + "\n  actual  =" + actual);
            }
        }

        assertEquals(500, cases);
        assertTrue(failures.isEmpty(), failures.size() + "건 불일치\n" + String.join("\n", failures.stream().limit(5).toList()));
    }

    // 같은 시각이면 플레이어가 먼저 공격 (둘 다 공속 1, 플레이어 한 방에 처치)
    @Test
    void sameTime_playerAttacksFirst() {
        CombatStat player = new CombatStat(100, 1, 100, 0, 0);
        CombatStat enemy  = new CombatStat(1000, 1, 100, 0, 0);

        SubStageResult result = new BattleSimulator(1.5, 0).simulate(player, 100, enemy, 30, 1L, 0);

        assertEquals(BattleSubStageResultEnum.ENEMY_DEAD, result.resultEnum());
        assertEquals(1, result.playerAttackCount());
        assertEquals(0, result.enemyAttackCount());
        assertEquals(1.0, result.duration());
        assertEquals(100, result.playerHp());
    }

    // 처치 회복은 최대 체력을 넘지 않음
    @Test
    void healOnKill_cappedByMaxHp() {
        CombatStat player = new CombatStat(100, 1, 1000, 0, 0);
        CombatStat enemy  = new CombatStat(1, 0.1, 100, 0, 0);

        assertEquals(100,  new BattleSimulator(1.5, 0.25).simulate(player, 900, enemy, 30, 1L, 0).healOnKill());
        assertEquals(250,  new BattleSimulator(1.5, 0.25).simulate(player, 500, enemy, 30, 1L, 0).healOnKill());
    }

    // 클라 BattleSubStage.Advance는 "제한시간 < 공격 시각 <= 제한시간 + 1e-9"인 공격이 있으면 끝나지 않음
    // (공속 2.3의 69번째 공격 = 30.000000000000004). 서버는 그 공격까지 적용한 뒤 시간 초과로 끝냄
    @Test
    void attackJustAfterTimeLimit_appliedThenTimeOver() {
        CombatStat player = new CombatStat(1, 2.3, 1000, 0, 0);
        CombatStat enemy  = new CombatStat(1, 1, 1_000_000, 0, 0);

        SubStageResult result = new BattleSimulator(1.5, 0).simulate(player, 1000, enemy, 30, 42L, 0);

        assertEquals(BattleSubStageResultEnum.TIME_OVER, result.resultEnum());
        assertEquals(69, result.playerAttackCount());
        assertEquals(30, result.enemyAttackCount());
        assertEquals(30.0, result.duration());
    }

    private static double d(String value)    { return Double.parseDouble(value); }

    private static double bits(String value) { return Double.longBitsToDouble(Long.parseLong(value)); }

    private static boolean sameBits(SubStageResult a, SubStageResult b) {
        return a.resultEnum() == b.resultEnum()
                && a.playerAttackCount() == b.playerAttackCount()
                && a.enemyAttackCount()  == b.enemyAttackCount()
                && Double.doubleToLongBits(a.playerHp())   == Double.doubleToLongBits(b.playerHp())
                && Double.doubleToLongBits(a.enemyHp())    == Double.doubleToLongBits(b.enemyHp())
                && Double.doubleToLongBits(a.duration())   == Double.doubleToLongBits(b.duration())
                && Double.doubleToLongBits(a.healOnKill()) == Double.doubleToLongBits(b.healOnKill());
    }
}
