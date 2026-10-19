package com.swordmaster.shop.iap.google;

import java.time.Instant;
import java.util.List;

// 구글 결제 서버와 통신하는 창구 (구현체를 바꿔 끼울 수 있도록 인터페이스로 분리)
public interface GooglePlayVerifier {

    // 영수증 조회
    // 존재하지 않는 영수증이면 InvalidPurchaseException, 통신 장애면 GooglePlayUnavailableException
    GooglePurchase verify(String productId, String purchaseToken);

    // 소모성 상품 소비 처리 (acknowledge 포함, 3일 안에 해야 자동 환불되지 않음)
    void consume(String productId, String purchaseToken);

    // 지정 시각 이후 환불·취소된 구매의 purchaseToken 목록
    List<String> voidedTokensSince(Instant since);

    // 클라이언트 잘못: 위조, 다른 상품, 존재하지 않는 영수증 → 400
    class InvalidPurchaseException extends RuntimeException {
        public InvalidPurchaseException(String message) {
            super(message);
        }
    }

    // 구글 쪽 장애, 네트워크 실패 → 503 (클라이언트가 같은 영수증으로 재전송)
    class GooglePlayUnavailableException extends RuntimeException {
        public GooglePlayUnavailableException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}