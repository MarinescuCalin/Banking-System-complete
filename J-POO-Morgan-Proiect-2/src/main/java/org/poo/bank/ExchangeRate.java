package org.poo.bank;

import lombok.Getter;
import org.poo.fileio.ExchangeInput;

@Getter
public class ExchangeRate {
    private final String from;
    private final String to;
    private final double rate;

    public ExchangeRate(final ExchangeInput exchangeInput) {
        this.from = exchangeInput.getFrom();
        this.to = exchangeInput.getTo();
        this.rate = exchangeInput.getRate();
    }
}
