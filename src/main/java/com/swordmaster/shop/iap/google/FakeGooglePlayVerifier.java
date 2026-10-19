package com.swordmaster.shop.iap.google;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

// 로컬 테스트 전용 가짜 구글 검증기 (local 프로필 + google-play.fake=true 일 때만 등록)
// 토큰 문자열의 앞부분으로 상황을 흉내 낸다 ({userId} 자리에 내 계정 ID)
//   ok-{userId}-{아무값}      → 결제 완료
//   pending-{userId}-{아무값} → 결제 보류
//   nocons-{userId}-{아무값}  → 결제 완료, 단 consume은 항상 실패
//   invalid-{아무값}          → 존재하지 않는 영수증
//   down-{아무값}             → 구글 장애
@Slf4j
@Component
@Profile("local")
@ConditionalOnProperty(name = "google-play.fake", havingValue = "true")
public class FakeGooglePlayVerifier implements GooglePlayVerifier {
    private final Set<String> consumed = ConcurrentHashMap.newKeySet();    // 서버 재시작 시 초기화
    private final Set<String> voided   = ConcurrentHashMap.newKeySet();

    public FakeGooglePlayVerifier() {
        log.warn("[IAP] 가짜 구글 검증기가 활성화되었습니다. 운영 환경이라면 즉시 확인하세요.");
    }

    @Override
    public GooglePurchase verify(String productId, String purchaseToken) {
        String[] parts = purchaseToken.split("-", 3);     // [종류, userId, 나머지]
        String   kind  = parts[0];

        return switch (kind) {
            case "invalid" -> throw new InvalidPurchaseException("가짜: 존재하지 않는 영수증");
            case "down"    -> throw new GooglePlayUnavailableException("가짜: 구글 장애", null);
            case "ok", "pending", "nocons" -> new GooglePurchase(
                    kind.equals("pending") ? 2 : 0,
                    consumed.contains(purchaseToken) ? 1 : 0,
                    "GPA.FAKE-" + Math.abs(purchaseToken.hashCode()),
                    parts.length > 1 ? parts[1] : null,
                    Instant.now());
            default -> throw new InvalidPurchaseException("가짜: 알 수 없는 토큰 형식");
        };
    }

    @Override
    public void consume(String productId, String purchaseToken) {
        if (purchaseToken.startsWith("nocons-"))
            throw new GooglePlayUnavailableException("가짜: consume 실패", null);

        consumed.add(purchaseToken);
    }

    @Override
    public List<String> voidedTokensSince(Instant since) {
        return List.copyOf(voided);
    }

    // 환불 흉내 (스케줄러 단계에서 사용)
    public void voidPurchase(String purchaseToken) {
        voided.add(purchaseToken);
    }
}