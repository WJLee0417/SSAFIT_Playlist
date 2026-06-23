package com.ssafy.ssafit.mapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class TestDatabaseSafetyTest {

    @Test
    void configuredMapperDatabaseMustBeDedicatedTestDatabase() {
        String jdbcUrl = System.getenv("SSAFIT_TEST_DB_URL");
        if (jdbcUrl == null || jdbcUrl.isBlank()) {
            return;
        }

        TestDatabaseGuard.TestDatabaseConfig config =
                TestDatabaseGuard.validate(jdbcUrl, "test-user", "test-password");
        assertThat(config.databaseName()).containsIgnoringCase("test");
    }

    @Test
    void developmentDatabaseUrlIsRejectedBeforeConnection() {
        assertThatThrownBy(() -> TestDatabaseGuard.validate(
                "jdbc:mysql://127.0.0.1:3306/ssafit_playlist",
                "ssafit",
                "password"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Refusing to run Mapper integration tests")
                .hasMessageContaining("must contain 'test'");
    }

    @Test
    void nonMysqlUrlIsRejectedBeforeConnection() {
        assertThatThrownBy(() -> TestDatabaseGuard.validate(
                "jdbc:h2:mem:ssafit_test",
                "ssafit_test",
                "password"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("must be a MySQL JDBC URL");
    }
}
