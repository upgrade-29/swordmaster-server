package com.swordmaster.gacha.service;

import com.swordmaster.common.BusinessException;
import com.swordmaster.gacha.GachaPicker;
import com.swordmaster.gacha.table.GachaCatalog;
import com.swordmaster.reward.Reward;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;

@RequiredArgsConstructor
@Service
public class GachaService {

    private static final int MAX_OPEN = 10; //한 가챠당 최대로 여는 횟수

    private final GachaCatalog gachaCatalog;
    private final GachaPicker gachaPicker;

    /** 추첨만 하고 지급은 하지 않는다. 지급은 RewardService 담당. */
    public List<Reward> open(String gachaCode, long count) {
        if (count < 1 || count > MAX_OPEN) {
            throw new BusinessException(HttpStatus.INTERNAL_SERVER_ERROR, "INVALID_GACHA_COUNT");
        }
        GachaCatalog.Pool pool = gachaCatalog.findPool(gachaCode)
                .orElseThrow(() -> new BusinessException(HttpStatus.INTERNAL_SERVER_ERROR,
                        "GACHA POOL NOT FOUND: " + gachaCode));
        return gachaPicker.pick(pool, Math.toIntExact(count));
    }
}
