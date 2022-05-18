package com.epam.deltix.data.connectors.hitbtc;

import com.epam.deltix.data.connectors.commons.DataConnectorSettings;
import com.epam.deltix.data.connectors.commons.annotations.ConnectorSettings;

@ConnectorSettings("HITBTC")
public class HitbtcConnectorSettings extends DataConnectorSettings {

    private String wsUrl = "wss://api.hitbtc.com/api/3/ws/public";

    public HitbtcConnectorSettings() {
        super();
    }

    public HitbtcConnectorSettings(String tbUrl, String stream, String wsUrl) {
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
