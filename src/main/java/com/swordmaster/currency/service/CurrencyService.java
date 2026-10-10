package com.swordmaster.currency.service;

import com.swordmaster.common.BusinessException;
import com.swordmaster.common.table.constant.ConstantKey;
import com.swordmaster.common.table.constant.ConstantTable;
import com.swordmaster.currency.CurrencyReason;
import com.swordmaster.currency.CurrencyType;
import com.swordmaster.currency.entity.CurrencyHistory;
import com.swordmaster.currency.repository.CurrencyHistoryRepository;
import com.swordmaster.player.entity.Player;
import com.swordmaster.currency.entity.PlayerCurrency;
import com.swordmaster.currency.repository.CurrencyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional
public class CurrencyService {
    private final CurrencyRepository        currencyRepository;
    private final CurrencyHistoryRepository currencyHistoryRepository;
    private final ConstantTable             constantTable;

    // 새 플레이어 생성 시 해당 플레이어의 모든 재화 행 생성
    public Map<CurrencyType, Long> createAll(Player player) {
        Map<CurrencyType, Long> result = new EnumMap<>(CurrencyType.class);

        for (CurrencyType type : CurrencyType.values()) {
            PlayerCurrency currency = new PlayerCurrency(player, type);

            // 초기 자본금 지급
            if (currency.getType() == CurrencyType.GOLD) {
                long amount = constantTable.getLong(ConstantKey.INITIAL_GOLD);      // 상수 테이블에서 해당 값을 불러옴

                grant(player, type, amount, CurrencyReason.PLAYER_CREATE);          // 증가 후 내역도 같이 저장
            }
            else currencyRepository.save(currency);

            result.put(currency.getType(), currency.getAmount());
        }

        return result;
    }

    @Transactional(readOnly = true)
    // 조회 (전체)
    public Map<CurrencyType, Long> getAll(Player player) {
        Map<CurrencyType, Long> result     = new EnumMap<>(CurrencyType.class);
        List<PlayerCurrency>    currencies = currencyRepository.findAllByPlayer(player);

        for (PlayerCurrency currency : currencies)
            result.put(currency.getType(), currency.getAmount());

        return result;
    }

    // 변경 (지급)
    public long grant(Player player, CurrencyType type, long value, CurrencyReason reason) {
        checkPositive(value);   // 양수인지 확인

        PlayerCurrency currency = find(player, type);

        currency.add(value);

        return saveAndRecord(currency, value, reason, null);    // 저장 후 결과(amount) 반환
    }

    // 변경 (차감)
    public long consume(Player player, CurrencyType type, long value, CurrencyReason reason) {
        checkPositive(value);   // 양수인지 확인

        PlayerCurrency currency = find(player, type);

        if (!currency.has(value)) throw new BusinessException(type + "이(가) 부족합니다.");

        currency.subtract(value);

        return saveAndRecord(currency, -value, reason, null);    // 음수 값
    }

    // 변경 (관리자)
    public long adjust(Player player, CurrencyType type, long targetAmount, String memo) {
        if (targetAmount < 0)               throw new BusinessException("조정할 값이 음수입니다.");
        if (memo == null || memo.isBlank()) throw new BusinessException("메모를 입력해주세요.");

        PlayerCurrency currency = find(player, type);
        long           delta    = targetAmount - currency.getAmount();

        if      (delta == 0) return currency.getAmount();    // 변동 없이 종료
        else if (delta > 0)  currency.add(delta);
        else                 currency.subtract(-delta);

        return saveAndRecord(currency, delta, CurrencyReason.ADMIN_ADJUST, memo);
    }

    // 공통 (조회)
    private PlayerCurrency find(Player player, CurrencyType type) {
        return currencyRepository.findByPlayerAndType(player, type)
                .orElseThrow(() -> new BusinessException(
                        HttpStatus.INTERNAL_SERVER_ERROR, type + "의 재화 정보가 없습니다."
                ));
    }

    // 공통 (조회, 유효성 검사)
    // 요구하는 변화량은 마이너스(또는 0)가 될수 없어야 함 (발생하면 서버 내의 코드 에러)
    private void checkPositive(long value) {
        if (value <= 0) {
            throw new BusinessException(
                    HttpStatus.INTERNAL_SERVER_ERROR, "재화 변동량은 0보다 커야 합니다. 입력값: " + value
            );
        }
    }

    // 공통 (저장 후에 결과 반환)
    private long saveAndRecord(PlayerCurrency currency, long delta, CurrencyReason reason, String memo) {
        currencyRepository.save(currency);

        currencyHistoryRepository.save(
                new CurrencyHistory(
                        currency.getPlayer().getId(),
                        currency.getType(),
                        delta,                  // 현재 변화량
                        currency.getAmount(),   // 잔액
                        reason,
                        memo
                )
        );

        return currency.getAmount();
    }
}
