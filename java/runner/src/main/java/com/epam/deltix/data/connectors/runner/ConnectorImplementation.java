package com.epam.deltix.data.connectors.runner;

import com.epam.deltix.data.connectors.commons.DataConnector;
import com.epam.deltix.data.connectors.commons.DataConnectorSettings;

class ConnectorImplementation<T extends DataConnector<S>, S extends DataConnectorSettings> {
    private final String name;
    private final Class<T> connectorClass;
    private final Class<S> settingsClass;

    ConnectorImplementation(String name, Class<T> connectorClass, Class<S> settingsClass) {
        this.name = name;
        this.connectorClass = connectorClass;
        this.settingsClass = settingsClass;
    }

    public Class<T> getConnectorClass() {
        return connectorClass;
    }

    public Class<S> getSettingsClass() {
        return settingsClass;
    }

    @Override
    public String toString() {
        return name + " [" + connectorClass.getSimpleName() + "(" + settingsClass.getSimpleName() + ")]";
    }
}
