package com.swordmaster.gacha;

import com.swordmaster.gacha.table.GachaCatalog.Pool;
import com.swordmaster.reward.Reward;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.random.RandomGenerator;


@Component
public class GachaPicker {
    private final RandomGenerator random;

    public GachaPicker() { this(new SecureRandom()); }      // 운영
    GachaPicker(RandomGenerator random) { this.random = random; }   // 테스트용 시드 주입

    public List<Reward> pick(Pool pool, int count) {
        List<Reward> results = new ArrayList<>(count);

        for (int i = 0; i < count; i++)
            results.add(pool.rewardAt(random.nextLong(pool.totalWeight())));

        return results;
    }
}