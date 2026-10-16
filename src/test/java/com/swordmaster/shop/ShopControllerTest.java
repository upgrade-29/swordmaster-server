package com.swordmaster.shop;

import com.swordmaster.common.BusinessException;
import com.swordmaster.common.GlobalExceptionHandler;
import com.swordmaster.currency.CurrencyErrorCode;
import com.swordmaster.currency.dto.CurrenciesResponse;
import com.swordmaster.reward.Reward;
import com.swordmaster.reward.RewardType;
import com.swordmaster.shop.controller.ShopController;
import com.swordmaster.shop.dto.ShopPurchaseResponse;
import com.swordmaster.shop.facade.ShopFacade;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;



// 상점 에러가 명세의 { code, message } 형식으로 응답되는지 확인 (구매 로직은 ShopPurchaseIntegrationTest)
class ShopControllerTest {
    private static final Long USER_ID = 7L;

    private final ShopFacade shopFacade = mock(ShopFacade.class);
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(USER_ID, null, List.of()));

        mockMvc = MockMvcBuilders.standaloneSetup(new ShopController(shopFacade))
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .build();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private static String body(String productCode) {
        return """
                { "requestId": "%s", "productCode": "%s" }
                """.formatted(UUID.randomUUID(), productCode);
    }

    @Test
    void 없는_상품이면_404와_PRODUCT_NOT_FOUND를_응답한다() throws Exception {
        when(shopFacade.purchase(eq(USER_ID), any()))
                .thenThrow(new BusinessException(ShopErrorCode.PRODUCT_NOT_FOUND));

        mockMvc.perform(post("/api/shop/purchase").contentType(MediaType.APPLICATION_JSON).content(body("NONE")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("PRODUCT_NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("상품을 찾을 수 없습니다."));
    }

    @Test
    void 다이아가_부족하면_400과_NOT_ENOUGH_DIAMOND를_응답한다() throws Exception {
        when(shopFacade.purchase(eq(USER_ID), any()))
                .thenThrow(new BusinessException(CurrencyErrorCode.NOT_ENOUGH_DIAMOND));

        mockMvc.perform(post("/api/shop/purchase").contentType(MediaType.APPLICATION_JSON).content(body("GOLD_GACHA_10")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("NOT_ENOUGH_DIAMOND"))
                .andExpect(jsonPath("$.message").value("다이아가 부족합니다."));
    }

    @Test
    void 골드가_부족하면_400과_NOT_ENOUGH_GOLD를_응답한다() throws Exception {
        when(shopFacade.purchase(eq(USER_ID), any()))
                .thenThrow(new BusinessException(CurrencyErrorCode.NOT_ENOUGH_GOLD));

        mockMvc.perform(post("/api/shop/purchase").contentType(MediaType.APPLICATION_JSON).content(body("ANY")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("NOT_ENOUGH_GOLD"))
                .andExpect(jsonPath("$.message").value("골드가 부족합니다."));
    }

    @Test
    void 성공하면_200과_rewards_currencies를_응답한다() throws Exception {
        when(shopFacade.purchase(eq(USER_ID), any())).thenReturn(new ShopPurchaseResponse(
                List.of(new Reward(RewardType.GOLD, null, 100)), new CurrenciesResponse(100L, 0L)));

        mockMvc.perform(post("/api/shop/purchase").contentType(MediaType.APPLICATION_JSON).content(body("GOLD_GACHA_1")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rewards[0].rewardType").value("GOLD"))
                .andExpect(jsonPath("$.rewards[0].amount").value(100))
                .andExpect(jsonPath("$.currencies.gold").value(100));
    }
}