package com.epam.deltix.data.connectors.kraken;

import com.epam.deltix.data.connectors.commons.DataConnectorSettings;
import com.epam.deltix.data.connectors.commons.annotations.ConnectorSettings;

@ConnectorSettings("Kraken-Spot")
public class KrakenSpotConnectorSettings extends DataConnectorSettings {

    private String wsUrl = "wss://ws.kraken.com";

    public KrakenSpotConnectorSettings() {
        super();
    }

    public KrakenSpotConnectorSettings(String tbUrl, String stream, String wsUrl) {
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
