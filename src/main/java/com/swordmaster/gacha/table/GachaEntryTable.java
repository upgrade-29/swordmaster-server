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
    // 가챠 코드별 결과 후보 목록 (시트 행 순서 유지)
    private final Map<String, List<GachaEntry>> byGacha;

    public GachaEntryTable(GoogleSheetClient sheets, @Value("${game-data.gid.gachaentry}") long gid) {
        super(sheets, gid, GachaEntry.class);    // 변환 후 검증까지 실행

        byGacha = rows.stream()
                .collect(Collectors.collectingAndThen(
                        Collectors.groupingBy(GachaEntry::gachaCode, Collectors.toUnmodifiableList()),
                        Map::copyOf
                ));
    }

    // 검증 (이 테이블 혼자 확인 가능한 것만, 유물 시트와의 교차 검증은 GachaCatalog)
    @Override
    protected void validate(List<String> errors) {
        // 같은 가챠 안에 같은 보상이 두 줄 있으면 확률 공개 시 혼란 → 금지
        checkUnique("gachaCode+rewardType+rewardCode+amount",
                e -> e.gachaCode() + "|" + e.rewardType() + "|" + e.rewardCode() + "|" + e.amount(), errors);

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

            // 상자 안에 상자 금지 (RewardService 재귀 호출이 끝나지 않는 것을 방지)
            if (entry.rewardType() == RewardType.GACHA_BOX)
                errors.add(at + "가챠 결과에 GACHA_BOX는 넣을 수 없습니다.");

            // 수량, 가중치는 0보다 커야 함
            if (entry.amount() <= 0) errors.add(at + "amount는 0보다 커야 합니다.");
            if (entry.weight() <= 0) errors.add(at + "weight는 0보다 커야 합니다.");

            // 재화형(GOLD, DIAMOND)은 rewardCode 없음, 나머지는 rewardCode 필수
            boolean noCode = entry.rewardCode() == null || entry.rewardCode().isBlank();

            if (entry.rewardType().isCurrency() && !noCode)
                errors.add(at + entry.rewardType() + "는 rewardCode를 비워야 합니다.");
            if (!entry.rewardType().isCurrency() && noCode)
                errors.add(at + entry.rewardType() + "는 rewardCode가 필요합니다.");
        }
    }

    // 검색
    // 결과 후보가 존재하는 가챠 코드 목록 (교차 검증용)
    public Set<String> gachaCodes() {
        return byGacha.keySet();
    }

    public List<GachaEntry> findEntries(String gachaCode) {
        return byGacha.getOrDefault(gachaCode, List.of());
    }
}