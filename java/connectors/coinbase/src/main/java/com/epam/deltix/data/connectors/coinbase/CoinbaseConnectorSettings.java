package com.epam.deltix.data.connectors.coinbase;

import com.epam.deltix.data.connectors.commons.DataConnectorSettings;
import com.epam.deltix.data.connectors.commons.annotations.ConnectorSettings;

@ConnectorSettings("Coinbase")
public class CoinbaseConnectorSettings extends DataConnectorSettings {

    private String wsUrl = "wss://ws-feed.pro.coinbase.com";

    public CoinbaseConnectorSettings() {
        super();
    }

    public CoinbaseConnectorSettings(String name, String wsUrl, String tbUrl, String stream) {
        super(tbUrl, stream);
        setName(name);
        this.wsUrl = wsUrl;
    }

    public String getWsUrl() {
        return wsUrl;
    }

    public void setWsUrl(String wsUrl) {
        this.wsUrl = wsUrl;
    }
}
