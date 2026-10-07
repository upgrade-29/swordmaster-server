package com.swordmaster.shop.service;

import com.swordmaster.common.BusinessException;
import com.swordmaster.shop.dto.ShopPurchaseRequest;
import com.swordmaster.shop.dto.ShopPurchaseResponse;
import com.swordmaster.shop.entity.ShopPurchaseLog;
import com.swordmaster.shop.repository.ShopPurchaseLogRepository;
import com.swordmaster.user.entity.User;
import com.swordmaster.user.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.Optional;

@RequiredArgsConstructor
@Service
public class ShopService {
    private final ShopPurchaseLogRepository shopPurchaseLogRepository;
    private final UserRepository userRepository;



    //아직 미구현 다른 역할의 패키지에서 참조할 부분이 많음
    @Transactional
    public ShopPurchaseResponse purchase(Long userId, ShopPurchaseRequest request){
        //1. 같은 사용자의 요청 두개가 겹치는 경우를 방지해서 사용자 조회를 하며 락을 걸어야한다.
        // 현재는 user 쪽에 재화 정보가 있다는 가정하에 코드작성 , 이후 player 쪽에서 재화를 관리한다면 이쪽으로 수정
        User user =userRepository.findById(userId) //todo : user 쪽에서 락을 걸 수 있는 항목이 필요(player 쪽으로 변경 가능성o)
                .orElseThrow(()->new BusinessException(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다."));

        //2. 이번에 받은 요청이 이전에 처리된 요청인지 확인하고 있다면 그 요청만 반환하고 메서드를 끝낸다. (요청을 확인하는 기준은 userId와 UUID)
        Optional<ShopPurchaseLog> found =
                shopPurchaseLogRepository.findByUserIdAndRequestId(userId, request.requestId().toString());
        if (found.isPresent()) {
            return found.get().getResponseBody(); //이전에 처리된 요청을 반환
        }


        //3. GameService의 current 에서 요청사항의 상품코드를 확인하고 코드에 맞는 보상정보를 가져온다.
        //todo: 이후 구글 스프레드 시트에서 가져온 상품과 상품에 대한 보상 데이터를 가져오는 로직 필요
        //만약 요청으로 받은 상품 코드와 시트에서 가져온 데이터의 상품에 일치하는 것이 없다면
        //PRODUCT_NOT_FOUND 로 오류를 처리


        //4. 구매를 요청한 유저의 재화를 차감하고 보상을 지급
        //todo: player 쪽의 재화 변동을 반영하고 보상테이블대로 보상을 지급하는 로직이 필요
        //재화가 부족한 경우 골드라면 NOT_ENOUGH_GOLD 다이아라면 NOT_ENOUGH_DIAMOND 로 처리
        //일단 상품코드에 해당하는 시트의 상품이 어떤 재화를 소모하는지를 확인한다.
        //만약 사용하는 재화가 골드라면? 우선 현재 유저의 재화가 이 가격을 지불할 수 있는지를 확인(지금은 has를 썼는데 currencies 를 생각하면 아예 전체 재화를 들고오는게 나을지도
        //if(currencyService.has(userId,CurrencyType.GOLD,재화 가격)) throw new BusinessException(HttpStatus.Conflict, "NOT_ENOUGH_GOLD");
        //재화가 충분할 시 유저의 재화에서 가격만큼 차감을 시도
        //long left= currencyService.consume(userId,CurrencyType.GOLD,재화가격)
        //재화가 차감되었다면 유저에게 보상을 지급해야한다. 보상은 재화가 아닌 것들도 있으니 따로 구현이 필요


        //5.구매를 위한 다른 작업들이 전부 처리되었다면 구매기록을 저장하고 응답을 반환
        ShopPurchaseResponse response = new ShopPurchaseResponse(null,null); //todo : 시트 데이터와 이후 player 구현이 된다면 보상들과 유저의 남은 재화를 넣어준다.

        //구매 기록을 저장
        ShopPurchaseLog purchaseLog = shopPurchaseLogRepository.save(
                new ShopPurchaseLog(userId, request.requestId(), null, null, response) //todo: 시트 데이터를 불러온 후엔 시트의 상품,상품에 대한 보상을 넣어준다.
        );


        return response;
    }

}
