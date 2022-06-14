package com.epam.deltix.data.connectors.ascendex;

import com.epam.deltix.data.connectors.commons.DataConnectorSettings;
import com.epam.deltix.data.connectors.commons.annotations.ConnectorSettings;

@ConnectorSettings("Ascendex")
public class AscendexConnectorSettings extends DataConnectorSettings {
    private String wsUrl = "wss://ascendex.com/api/pro/v1/stream";

    AscendexConnectorSettings() {
        super();
    }

    public AscendexConnectorSettings(String name, String wsUrl, String tbUrl, String stream) {
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
