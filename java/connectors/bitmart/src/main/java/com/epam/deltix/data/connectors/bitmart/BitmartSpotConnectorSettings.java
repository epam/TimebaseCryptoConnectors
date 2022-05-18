package com.epam.deltix.data.connectors.bitmart;

import com.epam.deltix.data.connectors.commons.DataConnectorSettings;
import com.epam.deltix.data.connectors.commons.annotations.ConnectorSettings;

@ConnectorSettings("BITMART")
public class BitmartSpotConnectorSettings extends DataConnectorSettings {

    private String wsUrl = "wss://ws-manager-compress.bitmart.com/api?protocol=1.1";

    public BitmartSpotConnectorSettings() {
        super();
    }

    public BitmartSpotConnectorSettings(String tbUrl, String stream, String wsUrl) {
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
