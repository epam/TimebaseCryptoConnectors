package com.epam.deltix.data.connectors.deribit;

import com.epam.deltix.data.connectors.commons.DataConnectorSettings;
import com.epam.deltix.data.connectors.commons.annotations.ConnectorSettings;

@ConnectorSettings("Deribit")
public class DeribitConnectorSettings extends DataConnectorSettings {
    private String wsUrl = "wss://deribit.com/ws/api/v2";

    DeribitConnectorSettings() {
        super();
    }

    public DeribitConnectorSettings(String name, String wsUrl, String tbUrl, String stream) {
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
