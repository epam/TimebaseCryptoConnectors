package com.epam.deltix.data.connectors.binance.spot;

import com.epam.deltix.data.connectors.commons.DataConnectorSettings;
import com.epam.deltix.data.connectors.commons.annotations.ConnectorSettings;

@ConnectorSettings("Binance-Spot")
public class BinanceSpotConnectorSettings extends DataConnectorSettings {
    private String wsUrl = "wss://stream.binance.com:9443/ws";

    private String restUrl = "https://api.binance.com/api/v3";

    public BinanceSpotConnectorSettings() {
        super();
    }

    public BinanceSpotConnectorSettings(String tbUrl, String stream, String wsUrl) {
        super(tbUrl, stream);
        this.wsUrl = wsUrl;
    }

    public String getWsUrl() {
        return wsUrl;
    }

    public void setWsUrl(String wsUrl) {
        this.wsUrl = wsUrl;
    }

    public String getRestUrl() {
        return restUrl;
    }

    public void setRestUrl(String restUrl) {
        this.restUrl = restUrl;
    }

}
