package com.epam.deltix.data.connectors.okx;

import com.epam.deltix.data.connectors.commons.DataConnectorSettings;
import com.epam.deltix.data.connectors.commons.annotations.ConnectorSettings;

@ConnectorSettings("OKX")
public class OkxSpotConnectorSettings extends DataConnectorSettings {

    private String wsUrl = "wss://ws.okex.com:8443/ws/v5/public";

    public OkxSpotConnectorSettings() {
        super();
    }

    public OkxSpotConnectorSettings(String tbUrl, String stream, String wsUrl) {
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
