package com.epam.deltix.data.connectors.coinflex;

import com.epam.deltix.data.connectors.commons.DataConnectorSettings;
import com.epam.deltix.data.connectors.commons.annotations.ConnectorSettings;

@ConnectorSettings("COINFLEX")
public class CoinflexSpotConnectorSettings extends DataConnectorSettings {

    private String wsUrl = "wss://v2api.coinflex.com/v2/websocket";

    public CoinflexSpotConnectorSettings() {
        super();
    }

    public CoinflexSpotConnectorSettings(String tbUrl, String stream, String wsUrl) {
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
