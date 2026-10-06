package com.swordmaster.shop.facade;

import com.swordmaster.common.BusinessException;
import com.swordmaster.shop.dto.ShopPurchaseRequest;
import com.swordmaster.shop.dto.ShopPurchaseResponse;
import com.swordmaster.shop.service.ShopService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.sql.SQLException;


@Slf4j
@RequiredArgsConstructor
@Component
public class ShopFacade {

    private static final int MAX_RETRY = 2; //총 시행 횟수는 3
    private static final int MYSQL_DEADLOCK = 1213; //데드락 발생시 반환되는 에러코드

    private final ShopService shopService;

    public ShopPurchaseResponse purchase(Long userId, ShopPurchaseRequest request) {
        for (int retry = 0; ; retry++) {
            try {

                return shopService.purchase(userId, request);
            } catch (PessimisticLockingFailureException e) {
                if (retry >= MAX_RETRY || !isDeadlock(e)) {
                    log.warn("상점 구매 락 충돌로 실패: userId={}, requestId={}", userId, request.requestId(), e);
                    throw new BusinessException(HttpStatus.SERVICE_UNAVAILABLE,"요청이 몰려 처리하지 못했습니다. 잠시 후 다시 시도해 주세요.");
                }
            }

            log.warn("데드락 재시도 {}/{} userId={}",retry+1, MAX_RETRY, userId);
        }
    }


    private boolean isDeadlock(Throwable e) {
        for (Throwable t = e; t != null; t = t.getCause()) {
            if (t instanceof SQLException sql && sql.getErrorCode() == MYSQL_DEADLOCK) {
                return true;
            }
        }
        return false;
    }


}
