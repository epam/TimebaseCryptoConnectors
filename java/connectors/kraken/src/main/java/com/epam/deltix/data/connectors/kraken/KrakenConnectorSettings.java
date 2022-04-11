package com.epam.deltix.data.connectors.kraken;

import com.epam.deltix.data.connectors.commons.DataConnectorSettings;
import com.epam.deltix.data.connectors.commons.annotations.ConnectorSettings;

@ConnectorSettings("Kraken")
public class KrakenConnectorSettings extends DataConnectorSettings {

    private String wsUrl = "wss://ws.kraken.com";
    private int depth = 20;

    public KrakenConnectorSettings() {
        super();
    }

    public KrakenConnectorSettings(String tbUrl, String stream, String wsUrl) {
        super(tbUrl, stream);
        this.wsUrl = wsUrl;
    }

    public String getWsUrl() {
        return wsUrl;
    }

    public void setWsUrl(String wsUrl) {
        this.wsUrl = wsUrl;
    }

    public int getDepth() {
        return depth;
    }

    public void setDepth(int depth) {
        this.depth = depth;
    }
}
