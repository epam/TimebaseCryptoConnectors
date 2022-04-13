package com.epam.deltix.data.connectors.okex;

import com.epam.deltix.data.connectors.commons.DataConnectorSettings;
import com.epam.deltix.data.connectors.commons.annotations.ConnectorSettings;

@ConnectorSettings("OKEX")
public class OkexSpotConnectorSettings extends DataConnectorSettings {

    private String wsUrl = "wss://ws.okex.com:8443/ws/v5/public";
    private int depth = 20;

    public OkexSpotConnectorSettings() {
        super();
    }

    public OkexSpotConnectorSettings(String tbUrl, String stream, String wsUrl) {
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
