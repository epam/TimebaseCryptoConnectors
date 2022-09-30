package com.epam.deltix.data.connectors.uniswapsubgraph;

import com.epam.deltix.data.connectors.commons.DataConnectorSettings;
import com.epam.deltix.data.connectors.commons.annotations.ConnectorSettings;

@ConnectorSettings("UNISWAP-SUBGRAPH")
public class UniswapConnectorSettings extends DataConnectorSettings {

    private String subgraphUrl = "https://api.thegraph.com/subgraphs/name/uniswap/uniswap-v3";
    private String uniswapApiUrl = "http://localhost:3001/";
    private int amount;

    public UniswapConnectorSettings() {
        super();
    }

    public UniswapConnectorSettings(final String name, final String tbUrl, final String stream) {
        super(tbUrl, stream);
        setName(name);
        setSubgraphUrl(subgraphUrl);
    }

    public String getSubgraphUrl() {
        return subgraphUrl;
    }

    public void setSubgraphUrl(String subgraphUrl) {
        this.subgraphUrl = subgraphUrl;
    }

    public String getUniswapApiUrl() {
        return uniswapApiUrl;
    }

    public void setUniswapApiUrl(String uniswapApiUrl) {
        this.uniswapApiUrl = uniswapApiUrl;
    }

    public int getAmount() {
        return amount;
    }

    public void setAmount(int amount) {
        this.amount = amount;
    }
}
