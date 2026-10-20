package com.swordmaster.gacha;

import com.swordmaster.gacha.table.GachaCatalog.Candidate;
import com.swordmaster.gacha.table.GachaCatalog.Pool;
import com.swordmaster.reward.Reward;
import com.swordmaster.reward.RewardType;
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
        List<Reward> results = new ArrayList<>();

        for (int i = 0; i < count; i++) {
            // 1단계: weight로 행 선택
            Candidate candidate = pool.candidateAt(random.nextLong(pool.totalWeight()));

            if (!candidate.isRandom()) {
                results.add(candidate.reward());
                continue;
            }

            // 2단계: 같은 등급 아티팩트 중 균등 난수, amount번 (1개씩)
            List<String> codes = candidate.randomCodes();

            for (long n = 0; n < candidate.reward().amount(); n++) {
                String code = codes.get(random.nextInt(codes.size()));
                results.add(new Reward(RewardType.ARTIFACT, code, 1));
            }
        }
        return results;
    }
}