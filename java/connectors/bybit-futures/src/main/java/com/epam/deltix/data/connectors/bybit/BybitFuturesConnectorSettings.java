package com.epam.deltix.data.connectors.bybit;

import com.epam.deltix.data.connectors.commons.DataConnectorSettings;
import com.epam.deltix.data.connectors.commons.annotations.ConnectorSettings;

@ConnectorSettings("Bybit-Futures")
public class BybitFuturesConnectorSettings extends DataConnectorSettings {

    private String wsUrl = "wss://stream.bybit.com/realtime_public";

    public BybitFuturesConnectorSettings() {
        super();
    }

    public BybitFuturesConnectorSettings(String tbUrl, String stream, String wsUrl) {
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
