package com.swordmaster.currency.repository;

import com.swordmaster.currency.entity.CurrencyHistory;
import com.swordmaster.currency.entity.PlayerCurrency;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface CurrencyHistoryRepository extends JpaRepository<CurrencyHistory, Long> {
    // 현재 잔액과 내역 합계가 다른 행 검색
    //  1. 히스토리 테이블에서 찾고자하는 플레이어(id)와 재화(type)가 일치하는 기록들을 조회 (서브 쿼리)
    //  2. 조회 결과를 총합으로 반환, 기록이 없으면 0으로 초기화하여 반환 (미초기화 시 null을 반환함)
    //  3. 재화 테이블에서 저장된 amount와 해당 기록들의 총합을 비교하여 일치하지 않는(<> equals !=) 결과들을 조회
    @Query("""
        SELECT c FROM PlayerCurrency c
        WHERE c.amount <> (
            SELECT coalesce(sum(h.amount), 0) FROM CurrencyHistory h
            WHERE h.playerId = c.player.id and h.type = c.type
            )
    """)
    List<PlayerCurrency> findMismatched();
}
