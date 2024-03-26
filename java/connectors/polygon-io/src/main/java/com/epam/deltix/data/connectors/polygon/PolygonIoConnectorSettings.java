package com.epam.deltix.data.connectors.polygon;

import com.epam.deltix.data.connectors.commons.DataConnectorSettings;
import com.epam.deltix.data.connectors.commons.annotations.ConnectorSettings;

@ConnectorSettings("POLYGON-IO")
public class PolygonIoConnectorSettings extends DataConnectorSettings {

    private String wsUrl = "wss://socket.polygon.io/stocks";

    private String apiKey;

    public PolygonIoConnectorSettings() {
        super();
    }

    public PolygonIoConnectorSettings(String tbUrl, String stream, String wsUrl) {
        super(tbUrl, stream);
        this.wsUrl = wsUrl;
    }

    public String getWsUrl() {
        return wsUrl;
    }

    public void setWsUrl(String wsUrl) {
        this.wsUrl = wsUrl;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }
}
