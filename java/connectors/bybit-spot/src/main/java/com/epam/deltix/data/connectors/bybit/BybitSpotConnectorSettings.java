package com.epam.deltix.data.connectors.bybit;

import com.epam.deltix.data.connectors.commons.DataConnectorSettings;
import com.epam.deltix.data.connectors.commons.annotations.ConnectorSettings;

@ConnectorSettings("Bybit-Spot")
public class BybitSpotConnectorSettings extends DataConnectorSettings {

    private String wsUrl = "wss://stream.bybit.com/spot/quote/ws/v1";
    private int depth = 20;

    public BybitSpotConnectorSettings() {
        super();
    }

    public BybitSpotConnectorSettings(String tbUrl, String stream, String wsUrl) {
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
