package com.swordmaster.shop;

import com.swordmaster.common.BusinessException;
import com.swordmaster.shop.dto.Currencies;
import com.swordmaster.shop.dto.ShopPurchaseRequest;
import com.swordmaster.shop.dto.ShopPurchaseResponse;
import com.swordmaster.shop.facade.ShopFacade;
import com.swordmaster.shop.service.ShopService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.http.HttpStatus;

import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;


@ExtendWith(MockitoExtension.class)
class ShopFacadeTest {

    @Mock ShopService shopService;        // 가짜 Service
    @InjectMocks
    ShopFacade shopFacade;   // 가짜 Service를 주입받은 진짜 Facade

    private final Long userId = 1L;
    private final ShopPurchaseRequest request = new ShopPurchaseRequest(UUID.randomUUID(), "TEST_001");
    private final ShopPurchaseResponse response = new ShopPurchaseResponse(List.of(), new Currencies(0L, 0));

    // MySQL 에러코드를 가진 락 예외를 만든다 (1213 = 데드락, 1205 = 락 대기 시간 초과)
    private static PessimisticLockingFailureException lockError(int mysqlErrorCode) {
        return new PessimisticLockingFailureException("lock",
                new SQLException("mysql error", "40001", mysqlErrorCode));
    }

    @Test
    void 성공하면_한번만_호출한다() {
        when(shopService.purchase(userId, request)).thenReturn(response);

        assertThat(shopFacade.purchase(userId, request)).isSameAs(response);
        verify(shopService, times(1)).purchase(userId, request);
    }

    @Test
    void 데드락이_두번_나고_세번째에_성공하면_응답을_반환한다() {
        when(shopService.purchase(userId, request))
                .thenThrow(lockError(1213), lockError(1213))
                .thenReturn(response);

        assertThat(shopFacade.purchase(userId, request)).isSameAs(response);
        verify(shopService, times(3)).purchase(userId, request);
    }

    @Test
    void 데드락이_세번_연속이면_503() {
        when(shopService.purchase(userId, request)).thenThrow(lockError(1213));

        assertThatThrownBy(() -> shopFacade.purchase(userId, request))
                .isInstanceOf(BusinessException.class)
                .extracting("status").isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        verify(shopService, times(3)).purchase(userId, request);
    }

    @Test
    void 데드락이_아닌_락_오류는_재시도하지_않고_503() {
        when(shopService.purchase(userId, request)).thenThrow(lockError(1205));

        assertThatThrownBy(() -> shopFacade.purchase(userId, request))
                .isInstanceOf(BusinessException.class)
                .extracting("status").isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        verify(shopService, times(1)).purchase(userId, request);
    }

    @Test
    void 락과_무관한_예외는_그대로_전파된다() {
        when(shopService.purchase(userId, request))
                .thenThrow(new BusinessException(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다."));

        assertThatThrownBy(() -> shopFacade.purchase(userId, request))
                .isInstanceOf(BusinessException.class)
                .extracting("status").isEqualTo(HttpStatus.NOT_FOUND);
        verify(shopService, times(1)).purchase(userId, request);
    }
}