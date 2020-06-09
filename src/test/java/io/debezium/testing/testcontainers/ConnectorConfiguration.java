/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.testing.testcontainers;

import java.util.List;

import org.testcontainers.containers.JdbcDatabaseContainer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.testcontainers.containers.KafkaContainer;

/**
 * Class that represents the config element of the configuration document.
 */
public class ConnectorConfiguration {

    private final ObjectMapper mapper = new ObjectMapper();
    private final ObjectNode configNode;

    protected ConnectorConfiguration() {
        this.configNode = this.mapper.createObjectNode();
        this.configNode.put("tasks.max", 1);
    }

    public static ConnectorConfiguration create() {
        return new ConnectorConfiguration();
    }

    static ConnectorConfiguration from(JsonNode configNode) {
        final ConnectorConfiguration configuration = new ConnectorConfiguration();
        configNode.fields().forEachRemaining(e -> configuration.configNode.set(e.getKey(), e.getValue()));
        return configuration;
    }

    private static final String CONNECTOR = "connector.class";
    private static final String HOSTNAME = "database.hostname";
    private static final String PORT = "database.port";
    private static final String USER = "database.user";
    private static final String PASSWORD = "database.password";
    private static final String DBNAME = "database.dbname";
    private static final String DBHISTORYTOPIC = "database.history.kafka.topic";
    private static final String DBHISTORYSERVERS = "database.history.kafka.bootstrap.servers";


    public static ConnectorConfiguration forJdbcContainer(JdbcDatabaseContainer<?> jdbcDatabaseContainer) {
        ConnectorConfiguration configuration = new ConnectorConfiguration();

        configuration.with(HOSTNAME, jdbcDatabaseContainer.getContainerInfo().getConfig().getHostName());

        final List<Integer> exposedPorts = jdbcDatabaseContainer.getExposedPorts();
        configuration.with(PORT, exposedPorts.get(0));

        configuration.with(USER, jdbcDatabaseContainer.getUsername());
        configuration.with(PASSWORD, jdbcDatabaseContainer.getPassword());

        final String driverClassName = jdbcDatabaseContainer.getDriverClassName();
        configuration.with(CONNECTOR, ConnectorResolver.getConnectorByJdbcDriver(driverClassName));

        // This property is valid for all databases except MySQL
        if (!isMySQL(driverClassName)) {
            configuration.with(DBNAME, jdbcDatabaseContainer.getDatabaseName());
        } else {
            configuration.with(DBHISTORYTOPIC, String.format("schema-changes.%s", jdbcDatabaseContainer.getDatabaseName()));
        }

        return configuration;
    }

    private static boolean isMySQL(String driverClassName) {
        return "com.mysql.cj.jdbc.Driver".equals(driverClassName) || "com.mysql.jdbc.Driver".equals(driverClassName);
    }

    public ConnectorConfiguration withKafkaForDatabaseHistory(KafkaContainer kafkaContainer) {
        with(DBHISTORYSERVERS, String.format("%s:9092", kafkaContainer.getNetworkAliases().get(0)));
        return this;
    }

    public ConnectorConfiguration with(String key, String value) {
        this.configNode.put(key, value);
        return this;
    }

    public ConnectorConfiguration with(String key, Integer value) {
        this.configNode.put(key, value);
        return this;
    }

    public ConnectorConfiguration with(String key, Long value) {
        this.configNode.put(key, value);
        return this;
    }

    public ConnectorConfiguration with(String key, Boolean value) {
        this.configNode.put(key, value);
        return this;
    }

    public ConnectorConfiguration with(String key, Double value) {
        this.configNode.put(key, value);
        return this;
    }

    ObjectNode getConfiguration() {
        return configNode;
    }
}
