package com.epam.deltix.data.connectors.runner.settings;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@ConfigurationProperties("")
public class ConnectorsSettingsProvider {

    private Map<String, Map<String, Object>> connectors;

    public Map<String, Map<String, Object>> getConnectors() {
        return connectors;
    }

    public void setConnectors(Map<String, Map<String, Object>> connectors) {
        this.connectors = new HashMap<>();
        connectors.forEach((key, value) -> this.connectors.put(key.toLowerCase(), value));
    }

    public Map<String, Object> connectorSettings(String connector) {
        return this.connectors.get(connector.toLowerCase());
    }

    public String extractType(String connector) {
        return extractString(connector, "type");
    }

    public String extractInstruments(String connector) {
        return extractString(connector, "instruments");
    }

    public List<String> extractModel(String connector) {
        return extractStringList(connector, "model");
    }

    public List<String> extractModelTypes(String connector) {
        return extractStringList(connector, "modelTypes");
    }

    public String extractString(String connector, String key) {
        Map<String, Object> connectorSettings = connectorSettings(connector);
        if (connectorSettings != null) {
            Object value = connectorSettings.get(key);
            if (value instanceof String) {
                return (String) value;
            }
        }

        return null;
    }

    public List<String> extractStringList(String connector, String key) {
        Map<String, Object> connectorSettings = connectorSettings(connector);
        if (connectorSettings != null) {
            Object value = connectorSettings.get(key);
            if (value instanceof Map) {
                return ((Map<?, ?>) value).values().stream()
                    .filter(String.class::isInstance)
                    .map(String.class::cast)
                    .collect(Collectors.toList());
            } else if (value instanceof List) {
                return ((List<?>) value).stream()
                    .filter(String.class::isInstance)
                    .map(String.class::cast)
                    .collect(Collectors.toList());
            }
        }

        return null;
    }


}
