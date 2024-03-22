package com.epam.deltix.data.connectors.coinbase;

import com.epam.deltix.data.connectors.commons.DataConnectorSettings;
import com.epam.deltix.data.connectors.commons.annotations.ConnectorSettings;

@ConnectorSettings("Coinbase")
public class CoinbaseConnectorSettings extends DataConnectorSettings {

    private String wsUrl = "wss://ws-feed.pro.coinbase.com";

    private String apiKey;

    private String apiSecret;

    private String passphrase;

    public CoinbaseConnectorSettings() {
        super();
    }

    public CoinbaseConnectorSettings(String name, String wsUrl, String tbUrl, String stream) {
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

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getApiSecret() {
        return apiSecret;
    }

    public void setApiSecret(String apiSecret) {
        this.apiSecret = apiSecret;
    }

    public String getPassphrase() {
        return passphrase;
    }

    public void setPassphrase(String passphrase) {
        this.passphrase = passphrase;
    }
}
