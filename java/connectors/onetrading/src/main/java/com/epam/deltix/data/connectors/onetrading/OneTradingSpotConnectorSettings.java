package com.epam.deltix.data.connectors.onetrading;

import com.epam.deltix.data.connectors.commons.DataConnectorSettings;
import com.epam.deltix.data.connectors.commons.annotations.ConnectorSettings;

@ConnectorSettings("ONETRADING")
public class OneTradingSpotConnectorSettings extends DataConnectorSettings {

    private String wsUrl = "wss://streams.fast.onetrading.com?x-version=3";

    public OneTradingSpotConnectorSettings() {
        super();
    }

    public OneTradingSpotConnectorSettings(String tbUrl, String stream, String wsUrl) {
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
