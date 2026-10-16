package com.swordmaster.currency.dto;

//재화의 남은 양을 나타낸다.
public record CurrenciesResponse(
        long gold,
        long diamond
) {}
