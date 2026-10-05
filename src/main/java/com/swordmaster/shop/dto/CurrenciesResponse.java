package com.swordmaster.shop.dto;

//재화와 관련된 모든 응답에 공통적으로 사용되는 응답 DTO
//현재는 shop에 잠시 두고 있으나 재화 관련 패키지에 옮겨야 할 듯
public record CurrenciesResponse(
        long gold,
        int diamond
) {}
