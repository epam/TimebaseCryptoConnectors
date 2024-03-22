package com.epam.deltix.data.connectors.bybit;

import com.epam.deltix.data.connectors.commons.DataConnectorSettings;
import com.epam.deltix.data.connectors.commons.annotations.ConnectorSettings;

@ConnectorSettings("Bybit")
public class BybitConnectorSettings extends DataConnectorSettings {

    private String wsUrl = "wss://stream.bybit.com/spot/quote/ws/v1";

    public BybitConnectorSettings() {
        super();
    }

    public BybitConnectorSettings(String tbUrl, String stream, String wsUrl) {
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
