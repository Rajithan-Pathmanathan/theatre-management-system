package com.vijaya.theatre;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

@TestConfiguration(proxyBeanMethods = false)
class TestcontainersConfiguration {

	@Bean
	@ServiceConnection
	PostgreSQLContainer postgresContainer() {
		// pinned, not "latest" — reproducible CI builds, matches the Postgres version used in prod
		return new PostgreSQLContainer(DockerImageName.parse("postgres:16-alpine"));
	}

}
