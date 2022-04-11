package com.epam.deltix.data.connectors.runner;

import com.epam.deltix.data.connectors.commons.DataConnector;
import com.epam.deltix.data.connectors.commons.DataConnectorSettings;

class ConnectorImplementation<T extends DataConnector<S>, S extends DataConnectorSettings> {
    private final Class<T> connectorClass;
    private final Class<S> settingsClass;

    ConnectorImplementation(Class<T> connectorClass, Class<S> settingsClass) {
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
        return connectorClass.getSimpleName() + "(" + settingsClass.getSimpleName() + ")";
    }
}
