package com.swordmaster.shop.iap.google;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FakeGooglePlayVerifierTest {
    private final FakeGooglePlayVerifier verifier = new FakeGooglePlayVerifier();

    @Test
    void ok_토큰은_결제완료이고_계정ID가_담긴다() {
        GooglePurchase p = verifier.verify("CASH_DIAMOND_TEST", "ok-7-0001");

        assertThat(p.isPurchased()).isTrue();
        assertThat(p.isConsumed()).isFalse();
        assertThat(p.obfuscatedAccountId()).isEqualTo("7");
    }

    @Test
    void pending_토큰은_보류상태다() {
        assertThat(verifier.verify("CASH_DIAMOND_TEST", "pending-7-0002").isPending()).isTrue();
    }

    @Test
    void consume_후에는_소비됨으로_조회된다() {
        verifier.consume("CASH_DIAMOND_TEST", "ok-7-0003");

        assertThat(verifier.verify("CASH_DIAMOND_TEST", "ok-7-0003").isConsumed()).isTrue();
    }

    @Test
    void invalid와_down은_서로_다른_예외를_던진다() {
        assertThatThrownBy(() -> verifier.verify("X", "invalid-1"))
                .isInstanceOf(GooglePlayVerifier.InvalidPurchaseException.class);
        assertThatThrownBy(() -> verifier.verify("X", "down-1"))
                .isInstanceOf(GooglePlayVerifier.GooglePlayUnavailableException.class);
    }

    @Test
    void nocons_토큰은_consume이_실패한다() {
        assertThatThrownBy(() -> verifier.consume("X", "nocons-7-0004"))
                .isInstanceOf(GooglePlayVerifier.GooglePlayUnavailableException.class);
    }
}