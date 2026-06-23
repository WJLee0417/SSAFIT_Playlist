package com.ssafy.ssafit.e2e;

import java.net.URI;
import java.util.Locale;

import org.springframework.boot.EnvironmentPostProcessor;
import org.springframework.boot.SpringApplication;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Profiles;

public class E2eDatabaseSafetyEnvironmentPostProcessor
        implements EnvironmentPostProcessor, Ordered {

    private static final String URL_ENV = "SSAFIT_TEST_DB_URL";
    private static final String USERNAME_ENV = "SSAFIT_TEST_DB_USERNAME";
    private static final String PASSWORD_ENV = "SSAFIT_TEST_DB_PASSWORD";

    @Override
    public void postProcessEnvironment(
            ConfigurableEnvironment environment,
            SpringApplication application) {
        if (!environment.acceptsProfiles(Profiles.of("e2e"))) {
            return;
        }

        String jdbcUrl = requireEnvironmentVariable(URL_ENV);
        requireEnvironmentVariable(USERNAME_ENV);
        requireEnvironmentVariable(PASSWORD_ENV);

        if (!jdbcUrl.startsWith("jdbc:mysql://")) {
            throw new IllegalStateException(
                    "Refusing to start E2E Backend: "
                            + URL_ENV
                            + " must be a MySQL JDBC URL.");
        }

        URI mysqlUri;
        try {
            mysqlUri = URI.create(jdbcUrl.substring("jdbc:".length()));
        } catch (IllegalArgumentException exception) {
            throw new IllegalStateException(
                    "Refusing to start E2E Backend: invalid "
                            + URL_ENV
                            + ".",
                    exception);
        }

        String databaseName = mysqlUri.getPath().replaceFirst("^/", "");
        if (databaseName.isBlank()
                || !databaseName.toLowerCase(Locale.ROOT).contains("test")) {
            throw new IllegalStateException(
                    "Refusing to start E2E Backend against database '"
                            + databaseName
                            + "'. The database name must contain 'test'.");
        }
    }

    private String requireEnvironmentVariable(String name) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(
                    "E2E Backend requires environment variable "
                            + name
                            + ".");
        }
        return value;
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
