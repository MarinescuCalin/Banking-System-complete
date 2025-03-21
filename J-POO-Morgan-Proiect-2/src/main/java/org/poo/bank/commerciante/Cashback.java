package org.poo.bank.commerciante;

import lombok.Getter;

@Getter
public final class Cashback {
    private final double percentage;

    public Cashback(final double percentage) {
        this.percentage = percentage;
    }
}
