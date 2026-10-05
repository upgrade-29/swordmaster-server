package com.swordmaster.shop.service;

import com.swordmaster.shop.dto.ShopPurchaseRequest;
import com.swordmaster.shop.dto.ShopPurchaseResponse;
import com.swordmaster.shop.repository.ShopPurchaseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class ShopService {
    private final ShopPurchaseRepository shopPurchaseRepository;

    //아직 미구현 다른 역할의 패키지에서 참조할 부분이 많음
    public ShopPurchaseResponse purchase(Long userId, ShopPurchaseRequest request){
        throw new UnsupportedOperationException("Not supported yet.");
    }

}
