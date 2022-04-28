package com.epam.deltix.data.connectors.cryptofacilities;

import com.epam.deltix.data.connectors.commons.DataConnectorSettings;
import com.epam.deltix.data.connectors.commons.annotations.ConnectorSettings;

@ConnectorSettings("Cryptofacilities")
public class CryptofacilitiesConnectorSettings extends DataConnectorSettings {

    private String wsUrl = "wss://www.cryptofacilities.com/ws/v1";

    public CryptofacilitiesConnectorSettings() {
        super();
    }

    public CryptofacilitiesConnectorSettings(String wsUrl, String tbUrl, String stream) {
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
