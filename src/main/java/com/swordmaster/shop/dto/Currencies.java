package com.swordmaster.shop.dto;

//재화의 남은 양을 나타낸다.
//현재는 shop에 잠시 두고 있으나 재화 관련 패키지에 옮겨야 함
public record Currencies(
        long gold,
        int diamond
) {}
