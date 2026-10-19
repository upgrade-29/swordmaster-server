package com.swordmaster.shop.iap;

// 구글 결제 영수증의 처리 상태
public enum IapReceiptStatus {
    GRANTED,    // 지급 완료, 구글 consume 대기 (스케줄러 재시도 대상)
    CONSUMED,   // 지급과 consume 모두 완료
    REFUNDED    // 구글에서 환불·취소됨
}