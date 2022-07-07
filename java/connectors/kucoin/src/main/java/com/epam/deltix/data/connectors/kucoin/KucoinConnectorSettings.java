package com.epam.deltix.data.connectors.kucoin;

import com.epam.deltix.data.connectors.commons.DataConnectorSettings;
import com.epam.deltix.data.connectors.commons.annotations.ConnectorSettings;

@ConnectorSettings("Kucoin")
public class KucoinConnectorSettings extends DataConnectorSettings {
    private String wsUrl = "wss://ws-api.kucoin.com/endpoint";

    private String restUrl = "https://api.kucoin.com/api/v1";

    public KucoinConnectorSettings() {
        super();
    }

    public KucoinConnectorSettings(String tbUrl, String stream, String wsUrl) {
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
