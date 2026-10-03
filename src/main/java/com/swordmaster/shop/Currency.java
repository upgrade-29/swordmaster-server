package com.swordmaster.shop;

import lombok.Getter;

@Getter
public class Currency {
    private Long gold;
    private int diamond;

    public Currency(Long gold, int diamond) {
        this.gold = gold;
        this.diamond = diamond;
    }
}
