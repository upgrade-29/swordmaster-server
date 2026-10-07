package com.swordmaster.currency.service;

import com.swordmaster.common.BusinessException;
import com.swordmaster.currency.CurrencyReason;
import com.swordmaster.currency.CurrencyType;
import com.swordmaster.currency.entity.CurrencyHistory;
import com.swordmaster.currency.repository.CurrencyHistoryRepository;
import com.swordmaster.player.entity.Player;
import com.swordmaster.player.entity.PlayerCurrency;
import com.swordmaster.player.repository.PlayerCurrencyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CurrencyService {
    private final PlayerCurrencyRepository  playerCurrencyRepository;
    private final CurrencyHistoryRepository currencyHistoryRepository;

    // 새 플레이어 생성 시 해당 플레이어의 모든 재화 행 생성
    @Transactional
    public Map<CurrencyType, Long> createAll(Player player) {
        Map<CurrencyType, Long> result = new EnumMap<>(CurrencyType.class);

        for (CurrencyType type : CurrencyType.values()) {
            PlayerCurrency currency = playerCurrencyRepository.save(new PlayerCurrency(player, type));

            result.put(currency.getType(), currency.getAmount());
        }

        return result;
    }

    // 조회 (전체)
    public Map<CurrencyType, Long> getAll(Long userId) {
        Map<CurrencyType, Long> result     = new EnumMap<>(CurrencyType.class);
        List<PlayerCurrency>    currencies = playerCurrencyRepository.findAllByPlayer_User_Id(userId);

        for (PlayerCurrency currency : currencies)
            result.put(currency.getType(), currency.getAmount());

        return result;
    }

    // 조회 (단일)
    public long getAmount(Long userId, CurrencyType type) {
        return find(userId, type).getAmount();
    }

    // 조회 (유효성 검사)
    public boolean has(Long userId, CurrencyType type, long value) {
        return find(userId, type).has(value);
    }

    // 변경 (지급)
    @Transactional
    public long grant(Long userId, CurrencyType type, long value, CurrencyReason reason) {
        checkPositive(value);   // 양수인지 확인

        PlayerCurrency currency = find(userId, type);

        currency.add(value);

        return saveAndRecord(currency, value, reason, null);    // 저장 후 결과(amount) 반환
    }

    // 변경 (차감)
    @Transactional
    public long consume(Long userId, CurrencyType type, long value, CurrencyReason reason) {
        checkPositive(value);   // 양수인지 확인

        PlayerCurrency currency = find(userId, type);

        if (!currency.has(value))
            throw new BusinessException(type + "이(가) 부족합니다.");

        currency.subtract(value);

        return saveAndRecord(currency, -value, reason, null);    // 음수 값
    }

    // 변경 (관리자)
    @Transactional
    public long adjust(Long userId, CurrencyType type, long targetAmount, String memo) {
        if (targetAmount < 0)               throw new BusinessException("조정할 값이 음수입니다.");
        if (memo == null || memo.isBlank()) throw new BusinessException("메모를 입력해주세요.");

        PlayerCurrency currency = find(userId, type);
        long           delta    = targetAmount - currency.getAmount();

        if      (delta == 0) return currency.getAmount();    // 변동 없이 종료
        else if (delta > 0)  currency.add(delta);
        else                 currency.subtract(-delta);

        return saveAndRecord(currency, delta, CurrencyReason.ADMIN_ADJUST, memo);
    }

    // 공통 (조회)
    private PlayerCurrency find(Long userId, CurrencyType type) {
        return playerCurrencyRepository.findByPlayer_User_IdAndType(userId, type)
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
        playerCurrencyRepository.save(currency);

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
