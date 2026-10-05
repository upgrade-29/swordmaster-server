package com.swordmaster.shop.facade;

import com.swordmaster.shop.dto.ShopPurchaseRequest;
import com.swordmaster.shop.dto.ShopPurchaseResponse;
import com.swordmaster.shop.service.ShopService;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.stereotype.Component;

import java.sql.SQLException;


@RequiredArgsConstructor
@Component
public class ShopFacade {
    //개발 계획서에서 데드락 발생 시를 대비하여 facade 패턴을 사용하던 구간

    private static final int MAX_RETRY = 2; //최대 시행 횟수
    private static final int MYSQL_DEADLOCK = 1213; //데드락 발생시 반환되는 에러코드

    private final ShopService shopService;

    public ShopPurchaseResponse purchase(Long userId, ShopPurchaseRequest request) {
        for (int retry = 0; ; retry++) {
            try {
                return shopService.purchase(userId, request);
            } catch (PessimisticLockingFailureException e) {
                if (retry >= MAX_RETRY || !isDeadlock(e)) {
                    throw e;
                }
            }
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
