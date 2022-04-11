package com.epam.deltix.data.connectors.ftx;

import com.epam.deltix.data.connectors.commons.DataConnectorSettings;
import com.epam.deltix.data.connectors.commons.annotations.ConnectorSettings;

@ConnectorSettings("FTX")
public class FtxConnectorSettings extends DataConnectorSettings {

    private String wsUrl = "wss://ftx.com/ws";
    private int depth = 20;

    public FtxConnectorSettings() {
        super();
    }

    public FtxConnectorSettings(String tbUrl, String stream, String wsUrl) {
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
