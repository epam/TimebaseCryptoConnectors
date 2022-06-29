package com.epam.deltix.data.connectors.binance.futures;

import com.epam.deltix.data.connectors.commons.DataConnectorSettings;
import com.epam.deltix.data.connectors.commons.annotations.ConnectorSettings;

@ConnectorSettings("Binance-Futures")
public class BinanceFuturesConnectorSettings extends DataConnectorSettings {
    private String wsUrl = "wss://fstream.binance.com/stream";

    private String restUrl = "https://fapi.binance.com/fapi/v1";

    public BinanceFuturesConnectorSettings() {
        super();
    }

    public BinanceFuturesConnectorSettings(String tbUrl, String stream, String wsUrl, String restUrl) {
        super(tbUrl, stream);
        this.wsUrl = wsUrl;
        this.restUrl = restUrl;
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
