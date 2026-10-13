package com.swordmaster.currency.service;

import com.swordmaster.common.BusinessException;
import com.swordmaster.common.table.constant.ConstantKey;
import com.swordmaster.common.table.constant.ConstantTable;
import com.swordmaster.currency.CurrencyErrorCode;
import com.swordmaster.currency.CurrencyReason;
import com.swordmaster.currency.CurrencyType;
import com.swordmaster.currency.dto.CurrenciesResponse;
import com.swordmaster.currency.entity.CurrencyHistory;
import com.swordmaster.currency.entity.PlayerCurrency;
import com.swordmaster.currency.repository.CurrencyHistoryRepository;
import com.swordmaster.currency.repository.CurrencyRepository;
import com.swordmaster.player.entity.Player;
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
    public CurrenciesResponse createAll(Player player) {
        Map<CurrencyType, Long> currencies = new EnumMap<>(CurrencyType.class);

        for (CurrencyType type : CurrencyType.values()) {
            PlayerCurrency currency = new PlayerCurrency(player, type);

            // 초기 자본금 지급
            if (currency.getType() == CurrencyType.GOLD) {
                long amount = constantTable.getLong(ConstantKey.INITIAL_GOLD);      // 상수 테이블에서 해당 값을 불러옴

                if (amount > 0) {
                    currency.add(amount);
                    saveAndRecord(currency, amount, CurrencyReason.PLAYER_CREATE, null);
                }
                else currencyRepository.save(currency);
            }
            else currencyRepository.save(currency);

            currencies.put(currency.getType(), currency.getAmount());
        }

        return toResponse(currencies);
    }

    // 조회 (전체)
    @Transactional(readOnly = true)
    public CurrenciesResponse getAll(Player player) {
        Map<CurrencyType, Long> currencies       = new EnumMap<>(CurrencyType.class);
        List<PlayerCurrency>    playerCurrencies = currencyRepository.findAllByPlayer(player);

        for (PlayerCurrency currency : playerCurrencies)
            currencies.put(currency.getType(), currency.getAmount());

        return toResponse(currencies);
    }

    // 변경 (지급)
    public CurrenciesResponse grant(Player player, CurrencyType type, long value, CurrencyReason reason) {
        // 양수인지 확인
        checkPositive(value);

        Map<CurrencyType, Long> currencies       = new EnumMap<>(CurrencyType.class);
        List<PlayerCurrency>    playerCurrencies = currencyRepository.findAllByPlayer(player);
        boolean                 isExist          = false;

        for (PlayerCurrency currency : playerCurrencies) {
            // 조회 후 지급
            if (currency.getType() == type) {
                currency.add(value);
                saveAndRecord(currency, value, reason, null);

                isExist = true;
            }

            currencies.put(currency.getType(), currency.getAmount());
        }

        if (!isExist) throw new BusinessException(HttpStatus.INTERNAL_SERVER_ERROR, type + "을(를) 찾을 수 없습니다.");

        return toResponse(currencies);
    }

    // 변경 (차감)
    public CurrenciesResponse consume(Player player, CurrencyType type, long value, CurrencyReason reason) {
        // 양수인지 확인
        checkPositive(value);

        Map<CurrencyType, Long> currencies       = new EnumMap<>(CurrencyType.class);
        List<PlayerCurrency>    playerCurrencies = currencyRepository.findAllByPlayer(player);
        boolean                 isExist          = false;

        for (PlayerCurrency currency : playerCurrencies) {
            // 조회 후 차감
            if (currency.getType() == type) {
                if (!currency.has(value)) {
                    // 재화의 사용 가능 여부(재화량) 검사
                    switch (type) {
                        case GOLD    -> throw new BusinessException(CurrencyErrorCode.NOT_ENOUGH_GOLD);
                        case DIAMOND -> throw new BusinessException(CurrencyErrorCode.NOT_ENOUGH_DIAMOND);
                        default      -> throw new BusinessException(type + "이(가) 부족합니다.");
                    }
                }

                currency.subtract(value);
                saveAndRecord(currency, -value, reason, null);      // 음수 값 저장

                isExist = true;
            }

            currencies.put(currency.getType(), currency.getAmount());
        }

        if (!isExist) throw new BusinessException(HttpStatus.INTERNAL_SERVER_ERROR, type + "을(를) 찾을 수 없습니다.");

        return toResponse(currencies);
    }

    // 공통 (조회, 유효성 검사)
    // 요구하는 변화량은 마이너스(또는 0)가 될수 없어야 함 (발생하면 서버 내의 코드 에러)
    private void checkPositive(long value) {
        if (value <= 0) {
            throw new BusinessException(HttpStatus.INTERNAL_SERVER_ERROR, "변화량(" + value + ")이 0보다 작거나 같습니다.");
        }
    }

    // 공통 (저장)
    private void saveAndRecord(PlayerCurrency currency, long delta, CurrencyReason reason, String memo) {
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
    }

    // 공통 (응답 변환)
    private CurrenciesResponse toResponse(Map<CurrencyType, Long> currencies) {
        return new CurrenciesResponse(
                currencies.getOrDefault(CurrencyType.GOLD,    0L),
                currencies.getOrDefault(CurrencyType.DIAMOND, 0L)
        );
    }
}
