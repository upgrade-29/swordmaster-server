package com.swordmaster.gacha.table;

import com.swordmaster.common.table.GoogleSheetClient;
import com.swordmaster.common.table.SheetTable;
import com.swordmaster.reward.RewardType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class GachaEntryTable extends SheetTable<GachaEntry> {
    // 가챠 코드별 결과 후보 목록 (같은 gachacode 끼리 묶음)
    private final Map<String, List<GachaEntry>> byGacha;

    public GachaEntryTable(GoogleSheetClient sheets, @Value("${game-data.gid.gachaentry}") long gid) {
        super(sheets, gid, GachaEntry.class);    // 변환 후 검증까지 실행

        byGacha = rows.stream()
                .collect(Collectors.collectingAndThen(
                        Collectors.groupingBy(GachaEntry::gachaCode, Collectors.toUnmodifiableList()),
                        Map::copyOf
                ));
    }

    // 이 테이블 혼자 확인 가능한 것만 검증. 유물 시트와의 교차 검증은 GachaCatalog
    @Override
    protected void validate(List<String> errors) {
        // 같은 가챠 안에 같은 보상이 두 줄 있으면 확률 공개 시 혼란 → 금지 (NONE과 빈칸은 같은 값)
        checkUnique("gachaCode+rewardType+rewardCode+grade+amount",
                e -> e.gachaCode() + "|" + e.rewardType() + "|" + e.rewardCode()
                        + "|" + (e.isRandomArtifact() ? e.equipmentGrade() : "-") + "|" + e.amount(),
                errors);

        for (int i = 0; i < rows.size(); i++) {
            GachaEntry entry = rows.get(i);

            // gachaCode는 Not Blank
            if (entry.gachaCode() == null || entry.gachaCode().isBlank()) {
                errors.add((i + 1) + "행의 gachaCode가 비어 있습니다.");
                continue;
            }

            String at = entry.gachaCode() + "의 " + (i + 1) + "행 ";

            // 보상 종류는 Not Null
            if (entry.rewardType() == null) {
                errors.add(at + "rewardType이 비어 있습니다.");
                continue;
            }

            // 가챠안에 가챠는 금지 (RewardService 재귀 호출이 끝나지 않는 것을 방지)
            if (entry.rewardType() == RewardType.GACHA_BOX)
                errors.add(at + "가챠 결과에 GACHA_BOX는 넣을 수 없습니다.");

            // 수량, 가중치는 0보다 커야 함
            if (entry.amount() <= 0) errors.add(at + "amount는 0보다 커야 합니다.");
            if (entry.weight() <= 0) errors.add(at + "weight는 0보다 커야 합니다.");

            boolean noCode   = entry.rewardCode() == null || entry.rewardCode().isBlank();
            boolean hasGrade = entry.equipmentGrade() != null;      // NONE·빈칸은 false

            // 등급은 현재 ARTIFACT 전용 (GOLD 등은 NONE 또는 빈칸)
            if (hasGrade && entry.rewardType() != RewardType.ARTIFACT)
                errors.add(at + entry.rewardType() + "는 grade를 NONE으로 두어야 합니다.");

            if (entry.rewardType().isCurrency()) {
                // 재화형(GOLD, DIAMOND)은 rewardCode 없음
                if (!noCode) errors.add(at + entry.rewardType() + "는 rewardCode를 비워야 합니다.");
            } else if (entry.rewardType() == RewardType.ARTIFACT) {
                // ARTIFACT는 rewardCode(지정) 또는 grade(랜덤) 중 최소 하나, 둘 다 있으면 지정 지급이며 grade는 무시
                if (noCode && !hasGrade) errors.add(at + "ARTIFACT는 rewardCode 또는 grade가 필요합니다.");
                // 등급 랜덤 방식의 행의 amount 는 1로 제한 (amount 만큼 결과값이 늘어나므로)
                if (noCode && hasGrade && entry.amount() != 1)
                    errors.add(at + "등급 랜덤 행의 amount는 1이어야 합니다. (횟수는 상품 보상 amount로 지정)");
            } else if (noCode) {
                // 나머지는 rewardCode 필수
                errors.add(at + entry.rewardType() + "는 rewardCode가 필요합니다.");
            }
        }
    }

    // 검색
    // 결과 후보가 존재하는 가챠 코드 목록 (교차 검증용)
    public Set<String> gachaCodes() {
        return byGacha.keySet();
    }

    // 특정 가챠의 행 목록
    public List<GachaEntry> findEntries(String gachaCode) {
        return byGacha.getOrDefault(gachaCode, List.of());
    }
}