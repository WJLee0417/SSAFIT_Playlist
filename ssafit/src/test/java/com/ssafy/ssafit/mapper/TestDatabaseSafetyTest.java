package com.ssafy.ssafit.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;

import org.junit.jupiter.api.Test;

class TestDatabaseSafetyTest {

    @Test
    void configuredMapperDatabaseMustBeDedicatedTestDatabase() {
        String jdbcUrl = System.getenv("SSAFIT_TEST_DB_URL");
        if (jdbcUrl == null || jdbcUrl.isBlank()) {
            return;
        }

        assertThat(jdbcUrl)
                .as("Mapper integration tests require a MySQL JDBC URL")
                .startsWith("jdbc:mysql://");

        URI mysqlUri = URI.create(jdbcUrl.substring("jdbc:".length()));
        String databaseName = mysqlUri.getPath().replaceFirst("^/", "");

        assertThat(databaseName.toLowerCase())
                .as("Refusing to use a database without 'test' in its name")
                .contains("test");
    }
}
