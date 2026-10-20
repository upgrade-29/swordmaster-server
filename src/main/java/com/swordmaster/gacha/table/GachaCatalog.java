package com.swordmaster.gacha.table;

import com.swordmaster.equipment.EquipmentGrade;
import com.swordmaster.equipment.artifact.table.Artifact;
import com.swordmaster.equipment.artifact.table.ArtifactTable;
import com.swordmaster.reward.Reward;
import com.swordmaster.reward.RewardType;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

@Component
public class GachaCatalog {
    //가챠 코드(키) -> 해당 가챠코드의 목록(값)
    private final Map<String, Pool> pools;

    // ArtifactTable은 부팅 때 artifact 시트를 이미 로드한 싱글톤 빈 (시트 재요청 없음)
    public GachaCatalog(GachaEntryTable entries, ArtifactTable artifacts) {
        List<String> errors = new ArrayList<>();

        // 등급별 아티팩트 코드 목록 (getAll()이 코드순 정렬 → 순서 고정, 테스트 재현 가능)
        Map<EquipmentGrade, List<String>> codesByGrade = artifacts.getAll().stream()
                .collect(Collectors.groupingBy(
                        Artifact::grade,
                        Collectors.mapping(Artifact::code, Collectors.toUnmodifiableList())));

        Map<String, Pool> built = new HashMap<>();

        //gachaCode 단위로 처리
        for (String gachaCode : entries.gachaCodes()) {
            List<GachaEntry> list = entries.findEntries(gachaCode);

            // 교차 검증: ARTIFACT 행만 대상
            for (GachaEntry entry : list) {
                // 아티팩트 타입이 아니면 다음 행으로 (교차 검증 대상 아님)
                if (entry.rewardType() != RewardType.ARTIFACT) continue;

                // 등급 랜덤(rewardType이 아티팩트이나 rewardCode가 없는 경우)인가?
                // 아티팩트 시트에 해당 등급의 아티팩트가 1개 이상 있어야 함 → 확인 후 다음 행으로
                if (entry.isRandomArtifact()) {
                    if (codesByGrade.getOrDefault(entry.equipmentGrade(), List.of()).isEmpty())
                        errors.add(gachaCode + ": " + entry.equipmentGrade() + " 등급 아티팩트가 없습니다.");
                    continue;
                }

                //아티팩트의 등급은 검사하지 않고 아티팩트의 시트에 있는 코드인지만 검사
                if (!artifacts.existsByCode(entry.rewardCode()))
                    errors.add(gachaCode + ": Artifact에 없는 코드 " + entry.rewardCode());
            }

            // 누적 가중치 계산 (같은 gachacode의 pool 내의 weight 합계가 long 범위를 넘으면 에러)
            try {
                built.put(gachaCode, Pool.of(gachaCode, list, codesByGrade));
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

    /** 추첨 후보. randomCodes가 비어 있으면 고정 보상, 있으면 그 등급 안에서 랜덤 */
    public record Candidate(Reward reward, List<String> randomCodes) {
        public boolean isRandom() { return !randomCodes.isEmpty(); }
    }

    /**
     * 추첨 한 번에 필요한 데이터 묶음.
     * cumulative 배열은 밖으로 내보내지 않고 candidateAt()으로만 조회 → 외부에서 수정 불가
     */
    public static final class Pool {
        private final String          gachaCode;
        private final List<Candidate> candidates;  // 후보 목록
        private final long[]          cumulative;  // 누적 가중치 예: [700, 950, 1000]

        private Pool(String gachaCode, List<Candidate> candidates, long[] cumulative) {
            this.gachaCode  = gachaCode;
            this.candidates = candidates;
            this.cumulative = cumulative;
        }

        static Pool of(String gachaCode, List<GachaEntry> entries,
                       Map<EquipmentGrade, List<String>> codesByGrade) {
            long[] cumulative = new long[entries.size()];
            long   sum        = 0;

            for (int i = 0; i < entries.size(); i++) {
                sum           = Math.addExact(sum, entries.get(i).weight());   // 오버플로 시 ArithmeticException
                cumulative[i] = sum;
            }

            // 등급 랜덤 행에만 후보 코드 목록을 붙임
            List<Candidate> candidates = entries.stream()
                    .map(e -> new Candidate(
                            e.toReward(),
                            e.isRandomArtifact()
                                    ? codesByGrade.getOrDefault(e.equipmentGrade(), List.of())
                                    : List.of()))
                    .toList();

            return new Pool(gachaCode, candidates, cumulative);
        }

        public String gachaCode()  { return gachaCode; }

        public long totalWeight()  { return cumulative[cumulative.length - 1]; }

        /** ticket: 0 이상 totalWeight 미만의 난수 → 해당 구간의 후보 */
        public Candidate candidateAt(long ticket) {
            int idx = Arrays.binarySearch(cumulative, ticket + 1);
            if (idx < 0) idx = -idx - 1;    // ticket+1 이상인 첫 칸
            return candidates.get(idx);
        }
    }
}