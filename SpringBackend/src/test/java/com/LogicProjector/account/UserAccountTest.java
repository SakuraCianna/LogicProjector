package com.LogicProjector.account;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class UserAccountTest {

    @Test
    void shouldRejectNegativeDebitAmount() {
        UserAccount user = new UserAccount(null, "teacher", "hash", 100, 0, "ACTIVE");

        assertThatThrownBy(() -> user.debitCredits(-10))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Credit amount must be positive");

        assertThat(user.getCreditsBalance()).isEqualTo(100);
        assertThat(user.getFrozenCreditsBalance()).isEqualTo(0);
    }

    @Test
    void shouldRejectNegativeFreezeAmount() {
        UserAccount user = new UserAccount(null, "teacher", "hash", 100, 0, "ACTIVE");

        assertThatThrownBy(() -> user.freezeCredits(-10))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Credit amount must be positive");

        assertThat(user.getCreditsBalance()).isEqualTo(100);
        assertThat(user.getFrozenCreditsBalance()).isEqualTo(0);
    }

    @Test
    void shouldRejectReleasingMoreCreditsThanFrozen() {
        UserAccount user = new UserAccount(null, "teacher", "hash", 82, 18, "ACTIVE");

        assertThatThrownBy(() -> user.releaseFrozenCredits(19))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Insufficient frozen credits to release");

        assertThat(user.getCreditsBalance()).isEqualTo(82);
        assertThat(user.getFrozenCreditsBalance()).isEqualTo(18);
    }

    @Test
    void shouldRejectSettlingMoreCreditsThanFrozen() {
        UserAccount user = new UserAccount(null, "teacher", "hash", 82, 18, "ACTIVE");

        assertThatThrownBy(() -> user.settleFrozenCredits(18, 19))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Insufficient frozen credits to settle");

        assertThat(user.getCreditsBalance()).isEqualTo(82);
        assertThat(user.getFrozenCreditsBalance()).isEqualTo(18);
    }
}
