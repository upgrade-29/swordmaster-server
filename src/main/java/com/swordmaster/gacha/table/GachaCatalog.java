package com.swordmaster.gacha.table;

import com.swordmaster.equipment.artifact.table.Artifact;
import com.swordmaster.equipment.artifact.table.ArtifactTable;
import com.swordmaster.reward.Reward;
import com.swordmaster.reward.RewardType;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

@Component
public class GachaCatalog {
    private final Map<String, Pool> pools;

    public GachaCatalog(GachaEntryTable entries, ArtifactTable artifacts) {
        List<String> errors = new ArrayList<>();

        // ArtifactTable에 findByCode가 없으므로 코드 집합을 직접 만듦
        Set<String> artifactCodes = artifacts.getAll().stream()
                .map(Artifact::code)
                .collect(Collectors.toUnmodifiableSet());

        Map<String, Pool> built = new HashMap<>();

        for (String gachaCode : entries.gachaCodes()) {
            List<GachaEntry> list = entries.findEntries(gachaCode);

            // 교차 검증: ARTIFACT 결과는 유물 시트에 존재해야 함
            for (GachaEntry entry : list)
                if (entry.rewardType() == RewardType.ARTIFACT && !artifactCodes.contains(entry.rewardCode()))
                    errors.add(gachaCode + ": Artifact에 없는 코드 " + entry.rewardCode());

            // 누적 가중치 계산 (합계가 long 범위를 넘으면 에러)
            try {
                built.put(gachaCode, Pool.of(gachaCode, list));
            } catch (ArithmeticException e) {
                errors.add(gachaCode + ": weight 합계가 너무 큽니다.");
            }
        }

        if (!errors.isEmpty())
            throw new IllegalStateException(
                    "GachaCatalog 검증에 실패하였습니다:\n - " + String.join("\n - ", errors)
            );

        pools = Map.copyOf(built);
    }

    // 검색
    public Optional<Pool> findPool(String gachaCode) {
        return Optional.ofNullable(pools.get(gachaCode));
    }

    /**
     * 추첨 한 번에 필요한 데이터 묶음.
     * cumulative 배열은 밖으로 내보내지 않고 rewardAt()으로만 조회 → 외부에서 수정 불가
     */
    public static final class Pool {
        private final String       gachaCode;
        private final List<Reward> rewards;     // 후보 목록
        private final long[]       cumulative;  // 누적 가중치 예: [700, 950, 1000]

        private Pool(String gachaCode, List<Reward> rewards, long[] cumulative) {
            this.gachaCode  = gachaCode;
            this.rewards    = rewards;
            this.cumulative = cumulative;
        }

        static Pool of(String gachaCode, List<GachaEntry> entries) {
            long[] cumulative = new long[entries.size()];
            long   sum        = 0;

            for (int i = 0; i < entries.size(); i++) {
                sum           = Math.addExact(sum, entries.get(i).weight());   // 오버플로 시 ArithmeticException
                cumulative[i] = sum;
            }

            List<Reward> rewards = entries.stream().map(GachaEntry::toReward).toList();

            return new Pool(gachaCode, rewards, cumulative);
        }

        public String gachaCode()   { return gachaCode; }

        public long totalWeight()   { return cumulative[cumulative.length - 1]; }

        /** ticket: 0 이상 totalWeight 미만의 난수 → 해당 구간의 보상 */
        public Reward rewardAt(long ticket) {
            int idx = Arrays.binarySearch(cumulative, ticket + 1);
            if (idx < 0) idx = -idx - 1;    // ticket+1 이상인 첫 칸

            return rewards.get(idx);
        }
    }
}