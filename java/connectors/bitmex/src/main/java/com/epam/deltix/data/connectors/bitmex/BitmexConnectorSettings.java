package com.epam.deltix.data.connectors.bitmex;

import com.epam.deltix.data.connectors.commons.DataConnectorSettings;
import com.epam.deltix.data.connectors.commons.annotations.ConnectorSettings;

@ConnectorSettings("Bitmex")
public class BitmexConnectorSettings extends DataConnectorSettings {

    private String wsUrl = "wss://ws.bitmex.com/realtime";
    private int depth = 20;

    public BitmexConnectorSettings() {
        super();
    }

    public BitmexConnectorSettings(String tbUrl, String stream, String wsUrl) {
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
