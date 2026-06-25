package com.ssafy.ssafit.mapper;

import java.net.URI;
import java.util.Locale;

final class TestDatabaseGuard {

    static final String URL_ENV = "SSAFIT_TEST_DB_URL";
    static final String USERNAME_ENV = "SSAFIT_TEST_DB_USERNAME";
    static final String PASSWORD_ENV = "SSAFIT_TEST_DB_PASSWORD";

    private TestDatabaseGuard() {
    }

    static TestDatabaseConfig requireFromEnvironment() {
        String jdbcUrl = requireEnvironmentVariable(URL_ENV);
        String username = requireEnvironmentVariable(USERNAME_ENV);
        String password = requireEnvironmentVariable(PASSWORD_ENV);
        return validate(jdbcUrl, username, password);
    }

    static TestDatabaseConfig validate(
            String jdbcUrl,
            String username,
            String password) {
        if (jdbcUrl == null || jdbcUrl.isBlank()) {
            throw new IllegalStateException(missingEnvironmentMessage(URL_ENV));
        }
        if (!jdbcUrl.startsWith("jdbc:mysql://")) {
            throw new IllegalStateException(
                    "Refusing to run Mapper integration tests: "
                            + URL_ENV
                            + " must be a MySQL JDBC URL.");
        }

        URI mysqlUri;
        try {
            mysqlUri = URI.create(jdbcUrl.substring("jdbc:".length()));
        } catch (IllegalArgumentException exception) {
            throw new IllegalStateException(
                    "Refusing to run Mapper integration tests: "
                            + URL_ENV
                            + " is not a valid JDBC URL.",
                    exception);
        }

        String databaseName = mysqlUri.getPath().replaceFirst("^/", "");
        if (databaseName.isBlank()
                || !databaseName.toLowerCase(Locale.ROOT).contains("test")) {
            throw new IllegalStateException(
                    "Refusing to run Mapper integration tests against database '"
                            + databaseName
                            + "'. The database name in "
                            + URL_ENV
                            + " must contain 'test'.");
        }
        if (username == null || username.isBlank()) {
            throw new IllegalStateException(
                    missingEnvironmentMessage(USERNAME_ENV));
        }
        if (password == null || password.isBlank()) {
            throw new IllegalStateException(
                    missingEnvironmentMessage(PASSWORD_ENV));
        }

        return new TestDatabaseConfig(
                jdbcUrl,
                username,
                password,
                databaseName);
    }

    private static String requireEnvironmentVariable(String name) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(missingEnvironmentMessage(name));
        }
        return value;
    }

    private static String missingEnvironmentMessage(String name) {
        return "Mapper integration tests require environment variable "
                + name
                + ". Configure a dedicated MySQL database whose name contains "
                + "'test'; integration tests are not skipped.";
    }

    record TestDatabaseConfig(
            String jdbcUrl,
            String username,
            String password,
            String databaseName) {
    }
}
