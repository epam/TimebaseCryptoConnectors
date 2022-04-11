package com.epam.deltix.data.connectors.huobi;

import com.epam.deltix.data.connectors.commons.DataConnectorSettings;
import com.epam.deltix.data.connectors.commons.annotations.ConnectorSettings;

@ConnectorSettings("Huobi-Spot")
public class HuobiSpotConnectorSettings extends DataConnectorSettings {

    private String wsUrl = "wss://api.huobi.pro/ws";
    private int depth = 20;

    public HuobiSpotConnectorSettings() {
        super();
    }

    public HuobiSpotConnectorSettings(String tbUrl, String stream, String wsUrl) {
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
