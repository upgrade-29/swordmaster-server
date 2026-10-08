package com.swordmaster.battle.simulation;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

// 기대값은 SplitMix64 표준 구현(unsigned 64비트)으로 계산한 값
// 클라이언트(C#) 구현도 같은 시드에서 같은 값이 나와야 함
class SplitMix64Test {

    @Test
    void nextLong_seed0() {
        SplitMix64 random = new SplitMix64(0L);

        assertEquals(Long.parseUnsignedLong("16294208416658607535"), random.nextLong());
        assertEquals(Long.parseUnsignedLong("7960286522194355700"),  random.nextLong());
        assertEquals(Long.parseUnsignedLong("487617019471545679"),   random.nextLong());
    }

    @Test
    void nextLong_seed1234567() {
        SplitMix64 random = new SplitMix64(1234567L);

        assertEquals(Long.parseUnsignedLong("6457827717110365317"), random.nextLong());
        assertEquals(Long.parseUnsignedLong("3203168211198807973"), random.nextLong());
        assertEquals(Long.parseUnsignedLong("9817491932198370423"), random.nextLong());
    }

    @Test
    void nextDouble_seed8412093746512038871() {
        SplitMix64 random = new SplitMix64(8412093746512038871L);

        assertEquals(0.07939609899825739, random.nextDouble());
        assertEquals(0.03405442445527984, random.nextDouble());
        assertEquals(0.40413342255123186, random.nextDouble());
    }

    @Test
    void forSubStage_sameAsSeedPlusIndex() {
        long battleSeed = 8412093746512038871L;

        for (int index = 0; index < 5; index++) {
            SplitMix64 expected = new SplitMix64(battleSeed + index);
            SplitMix64 actual   = SplitMix64.forSubStage(battleSeed, index);

            for (int i = 0; i < 3; i++) assertEquals(expected.nextLong(), actual.nextLong());
        }
    }

    @Test
    void forSubStage_overflowMatchesUnsigned() {
        // Long.MAX_VALUE + 1 은 Java에서 음수지만 C# ulong 9223372036854775808 과 같은 비트
        SplitMix64 actual   = SplitMix64.forSubStage(Long.MAX_VALUE, 1);
        SplitMix64 expected = new SplitMix64(Long.parseUnsignedLong("9223372036854775808"));

        assertEquals(expected.nextLong(), actual.nextLong());
    }

    @Test
    void nextDouble_range() {
        SplitMix64 random = new SplitMix64(42L);

        for (int i = 0; i < 100_000; i++) {
            double value = random.nextDouble();

            assertTrue(value >= 0.0 && value < 1.0, "범위를 벗어난 값: " + value);
        }
    }
}
