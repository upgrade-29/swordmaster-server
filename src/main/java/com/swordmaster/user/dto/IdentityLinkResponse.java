package com.swordmaster.user.dto;

import com.swordmaster.common.enums.LoginProvider;

public record IdentityLinkResponse(
        LoginProvider provider,
        boolean linked
) {
}
