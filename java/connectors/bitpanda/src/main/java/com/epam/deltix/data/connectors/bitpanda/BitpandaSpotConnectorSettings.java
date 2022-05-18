package com.epam.deltix.data.connectors.bitpanda;

import com.epam.deltix.data.connectors.commons.DataConnectorSettings;
import com.epam.deltix.data.connectors.commons.annotations.ConnectorSettings;

@ConnectorSettings("BITPANDA")
public class BitpandaSpotConnectorSettings extends DataConnectorSettings {

    private String wsUrl = "wss://streams.exchange.bitpanda.com";

    public BitpandaSpotConnectorSettings() {
        super();
    }

    public BitpandaSpotConnectorSettings(String tbUrl, String stream, String wsUrl) {
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
