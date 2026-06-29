package com.picker.BlinkitPicker.Services;

import com.picker.BlinkitPicker.Enums.RoleEnum;
import com.picker.BlinkitPicker.Model.UserModel;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class JwtServicesTest {

    private static final String SECRET = "!8!UejqyAxDT*Y!R9rE9WHU9RKts$^mAd4MWnTMFe%x*dkHJ7EfSaVw$SwkyJh74";

    @Test
    void generatedTokenCanBeVerifiedWithConfiguredSecret() {
        JwtServices jwtServices = new JwtServices(SECRET, 86_400_000L, 604_800_000L);
        UserModel user = UserModel.builder()
                .id(7L)
                .role(RoleEnum.ADMIN)
                .build();

        String token = jwtServices.generateAccessToken(user);

        Claims claims = jwtServices.validateAndExtractClaims(token);
        assertThat(claims.getIssuer()).isEqualTo("picker-administrator");
        assertThat(claims.getSubject()).isEqualTo("7");
        assertThat(claims.get("userId", Long.class)).isEqualTo(7L);
        assertThat(claims.get("role", String.class)).isEqualTo("ADMIN");
    }

    @Test
    void validationAcceptsBearerPrefix() {
        JwtServices jwtServices = new JwtServices(SECRET, 86_400_000L, 604_800_000L);
        UserModel user = UserModel.builder()
                .id(7L)
                .role(RoleEnum.ADMIN)
                .build();

        String token = jwtServices.generateAccessToken(user);

        assertThat(jwtServices.extractClaimsSafely("Bearer " + token)).isNotNull();
    }
}
