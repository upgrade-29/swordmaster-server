package com.swordmaster.player.repository;

import com.swordmaster.currency.CurrencyType;
import com.swordmaster.player.entity.PlayerCurrency;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PlayerCurrencyRepository extends JpaRepository<PlayerCurrency, Long> {
    // 플레이어 재화 전체 반환
    List<PlayerCurrency> findAllByPlayer_User_Id(Long userId);

    // 플레이어의 특정 재화 반환
    Optional<PlayerCurrency> findByPlayer_User_IdAndType(Long userId, CurrencyType type);

    // 재화의 종류 추가 시 모든 플레이어에 대하여 새로운 행을 생성
    @Modifying
    @Query(value = """
        -- 조회후 생성된 임시 값들을 테이블에 삽입
        INSERT INTO player_currencies (player_id, type, amount, version)
        -- 조건을 통해 해당 타입의 재화를 가지고 있지 않은 플레이어들을 조회 (값은 임시로 생성됨)
        SELECT p.id, :type, 0, 0
        FROM players p
        WHERE NOT EXISTS (
            -- 테이블에서 플레이어가 그 재화 타입을 가지고 있는지 조회
            SELECT 1 FROM player_currencies c
            WHERE c.player_id = p.id AND c.type = :type
            )
        """, nativeQuery = true)                            // 실제 DB에 보내는 SQL인지 표시
    int insertMissingRows(@Param("type") String type);      // 매개변수의 값을 SQL의 :키워드 뒤의 컬럼에 할당
}
