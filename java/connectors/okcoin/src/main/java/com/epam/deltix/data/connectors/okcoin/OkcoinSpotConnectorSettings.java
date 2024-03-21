package com.epam.deltix.data.connectors.okcoin;

import com.epam.deltix.data.connectors.commons.DataConnectorSettings;
import com.epam.deltix.data.connectors.commons.annotations.ConnectorSettings;

@ConnectorSettings("OKCOIN")
public class OkcoinSpotConnectorSettings extends DataConnectorSettings {

    private String wsUrl = "wss://real.okcoin.com:8443/ws/v5/public";

    public OkcoinSpotConnectorSettings() {
        super();
    }

    public OkcoinSpotConnectorSettings(String tbUrl, String stream, String wsUrl) {
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
