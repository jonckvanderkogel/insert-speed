package com.example.inserts

import org.slf4j.LoggerFactory
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.oracle.OracleContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers


@Testcontainers
@ActiveProfiles(profiles = ["test"])
abstract class AbstractIntegrationTest() {
    companion object {
        private val logger = LoggerFactory.getLogger(AbstractIntegrationTest::class.java)

        @Container
        val oracle = OracleContainer("gvenzl/oracle-free:23-slim-faststart")

        @JvmStatic
        @DynamicPropertySource
        fun registerPostgresProperties(registry: DynamicPropertyRegistry) {
            logger.info("Database name: ${oracle.databaseName}")
            logger.info("Port: ${oracle.firstMappedPort}")
            logger.info("Username: ${oracle.username}")
            logger.info("Password: ${oracle.password}")
            registry.add("spring.datasource.url") { "jdbc:oracle:thin:@localhost:${oracle.firstMappedPort}/${oracle.databaseName}" }
            registry.add("spring.datasource.username") { oracle.username }
            registry.add("spring.datasource.password") { oracle.password }
            registry.add("spring.liquibase.url") { "jdbc:oracle:thin:@localhost:${oracle.firstMappedPort}/${oracle.databaseName}" }
            registry.add("spring.liquibase.user") { oracle.username }
            registry.add("spring.liquibase.password") { oracle.password }
        }
    }
}

