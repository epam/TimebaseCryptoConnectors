package com.epam.deltix.data.connectors.kraken;

import com.epam.deltix.data.connectors.commons.DataConnectorSettings;
import com.epam.deltix.data.connectors.commons.annotations.ConnectorSettings;

@ConnectorSettings("Kraken-Futures")
public class KrakenFuturesConnectorSettings extends DataConnectorSettings {

    private String wsUrl = "wss://futures.kraken.com/ws/v1";
    private int depth = 20;

    public KrakenFuturesConnectorSettings() {
        super();
    }

    public KrakenFuturesConnectorSettings(String tbUrl, String stream, String wsUrl) {
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
