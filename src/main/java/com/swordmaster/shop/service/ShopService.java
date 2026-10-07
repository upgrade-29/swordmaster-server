package com.swordmaster.shop.service;

import com.swordmaster.common.BusinessException;
import com.swordmaster.currency.CurrencyReason;
import com.swordmaster.currency.CurrencyType;
import com.swordmaster.currency.dto.CurrenciesResponse;
import com.swordmaster.currency.service.CurrencyService;
import com.swordmaster.reward.Reward;
import com.swordmaster.reward.service.RewardService;
import com.swordmaster.shop.dto.ShopPurchaseRequest;
import com.swordmaster.shop.dto.ShopPurchaseResponse;
import com.swordmaster.shop.entity.ShopPurchaseLog;
import com.swordmaster.shop.repository.ShopPurchaseLogRepository;
import com.swordmaster.shop.table.ShopCatalog;
import com.swordmaster.shop.table.ShopProduct;
import com.swordmaster.user.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RequiredArgsConstructor
@Service
public class ShopService {
    private final ShopPurchaseLogRepository shopPurchaseLogRepository;
    private final UserRepository userRepository;
    private final ShopCatalog shopCatalog;
    private final CurrencyService currencyService;
    private final RewardService rewardService;


    @Transactional
    public ShopPurchaseResponse purchase(Long userId, ShopPurchaseRequest request){
        //1. 요청한 회원이 있는 지 확인한다.
        if (!userRepository.existsById(userId))
            throw new BusinessException(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다.");

        //2. 이번에 받은 요청이 이전에 처리된 요청인지 확인하고 있다면 그 요청만 반환하고 메서드를 끝낸다. (요청을 확인하는 기준은 userId와 UUID)
        Optional<ShopPurchaseLog> found =
                shopPurchaseLogRepository.findByUserIdAndRequestId(userId, request.requestId().toString());
        if (found.isPresent()) {
            return found.get().getResponseBody(); //이전에 처리된 요청을 반환
        }

        //3. GameService의 current 에서 요청사항의 상품코드를 확인하고 코드에 맞는 보상정보를 가져온다.
        ShopProduct product = shopCatalog.findProduct(request.productCode())
                .orElseThrow(()->new BusinessException(HttpStatus.NOT_FOUND,"PRODUCT NOT FOUND"));

        List<Reward> rewards = shopCatalog.findRewards(request.productCode());

        //4. 구매를 요청한 유저의 재화를 차감하고 보상을 지급
        CurrencyType priceType = product.priceType();

        //해당 유저에게 재화가 충분한지 검사
        if(!currencyService.has(userId, priceType,product.price()))
            throw new BusinessException(HttpStatus.CONFLICT,"NOT_ENOUGH_"+priceType.name());

        //유저의 재화에서 가격만큼 차감을 시도.
        currencyService.consume(userId, priceType,product.price(), CurrencyReason.PURCHASE);

        //재화가 차감되었다면 유저에게 보상을 지급.
        for(Reward reward : rewards){
            rewardService.reward(userId,reward,CurrencyReason.PURCHASE);
        }

        //차감 및 보상 완료 후의 재화를 재화 응답 dto 에 담음
        Map<CurrencyType,Long> left = currencyService.getAll(userId);
        CurrenciesResponse currenciesResponseDto = new CurrenciesResponse(
                left.get(CurrencyType.GOLD),
                left.get(CurrencyType.DIAMOND));


        //5.구매를 위한 다른 작업들이 전부 처리되었다면 구매기록을 저장하고 응답을 반환
        ShopPurchaseResponse response = new ShopPurchaseResponse(rewards, currenciesResponseDto);

        shopPurchaseLogRepository.save(new ShopPurchaseLog(userId, request.requestId(), product, rewards, response));

        return response;
    }

}
