package com.epam.deltix.data.connectors.bitfinex;

import com.epam.deltix.data.connectors.commons.DataConnectorSettings;
import com.epam.deltix.data.connectors.commons.annotations.ConnectorSettings;

@ConnectorSettings("BITFINEX")
public class BitfinexSpotConnectorSettings extends DataConnectorSettings {

    private String wsUrl = "wss://api.bitfinex.com/ws/2";

    public BitfinexSpotConnectorSettings() {
        super();
    }

    public BitfinexSpotConnectorSettings(String tbUrl, String stream, String wsUrl) {
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
