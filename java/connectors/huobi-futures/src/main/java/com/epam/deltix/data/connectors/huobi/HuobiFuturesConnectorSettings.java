package com.epam.deltix.data.connectors.huobi;

import com.epam.deltix.data.connectors.commons.DataConnectorSettings;
import com.epam.deltix.data.connectors.commons.annotations.ConnectorSettings;

@ConnectorSettings("Huobi-Futures")
public class HuobiFuturesConnectorSettings extends DataConnectorSettings {

    private String wsUrl = "wss://api.hbdm.com/ws";

    public HuobiFuturesConnectorSettings() {
        super();
    }

    public HuobiFuturesConnectorSettings(String tbUrl, String stream, String wsUrl) {
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
