package com.epam.deltix.data.connectors.poloniex;

import com.epam.deltix.data.connectors.commons.DataConnectorSettings;
import com.epam.deltix.data.connectors.commons.annotations.ConnectorSettings;

@ConnectorSettings("Poloniex")
public class PoloniexConnectorSettings extends DataConnectorSettings {

    private String wsUrl = "wss://ws.poloniex.com/ws/public";

    public PoloniexConnectorSettings() {
        super();
    }

    public PoloniexConnectorSettings(String wsUrl, String tbUrl, String stream) {
        super(tbUrl, stream);
        this.wsUrl = wsUrl;
    }

    public String getWsUrl() {
        return wsUrl;
    }

    public void setWsUrl(String wsUrl) {
        this.wsUrl = wsUrl;
    }
}
