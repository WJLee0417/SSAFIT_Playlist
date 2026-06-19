package com.ssafy.ssafit.common.auth;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class PasswordHashServiceTest {

    private final PasswordHashService passwordHashService = new PasswordHashService();

    @Test
    void encodedPasswordMatchesOnlyOriginalPassword() {
        String encoded = passwordHashService.encode("ssafit-password");

        assertThat(encoded).startsWith("pbkdf2_sha256$");
        assertThat(passwordHashService.matches("ssafit-password", encoded)).isTrue();
        assertThat(passwordHashService.matches("wrong-password", encoded)).isFalse();
    }
}
