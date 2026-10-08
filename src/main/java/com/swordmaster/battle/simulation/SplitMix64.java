package com.swordmaster.battle.simulation;

// 전투 검증용 난수 생성기 (클라이언트 C# 구현과 같은 시드면 같은 순서의 값을 생성해야 함)
// Java의 long 연산은 2의 보수 오버플로이므로 C#의 ulong 연산과 비트 단위로 같음 (시프트는 반드시 >>> 사용)
// 상태를 가지므로 스레드에 안전하지 않음 (전투 검증 1회마다 새로 생성)
public final class SplitMix64 {
    private static final long   GOLDEN_GAMMA = 0x9E3779B97F4A7C15L;
    private static final double DOUBLE_UNIT  = 0x1.0p-53;   // 1 / 2^53

    private long state;

    public SplitMix64(long seed) {
        state = seed;
    }

    // 세부 스테이지별 난수 생성기 (시드 = battleSeed + subStageIndex)
    // 세부 스테이지마다 독립적으로 재현되므로 이전 스테이지의 난수 상태를 저장할 필요가 없음
    // 합이 long 범위를 넘으면 음수가 되지만 비트는 C#의 ulong 덧셈 결과와 같음
    public static SplitMix64 forSubStage(long battleSeed, int subStageIndex) {
        return new SplitMix64(battleSeed + subStageIndex);
    }

    // 다음 64비트 값 (C#의 ulong과 같은 비트, Java에서는 음수로 보일 수 있음)
    public long nextLong() {
        long z = (state += GOLDEN_GAMMA);

        z = (z ^ (z >>> 30)) * 0xBF58476D1CE4E5B9L;
        z = (z ^ (z >>> 27)) * 0x94D049BB133111EBL;

        return z ^ (z >>> 31);
    }

    // 0 이상 1 미만의 실수 (상위 53비트 사용)
    public double nextDouble() {
        return (nextLong() >>> 11) * DOUBLE_UNIT;
    }
}
