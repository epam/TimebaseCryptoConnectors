package com.epam.deltix.data.connectors.uniswap;

import com.epam.deltix.data.connectors.commons.DataConnectorSettings;
import com.epam.deltix.data.connectors.commons.annotations.ConnectorSettings;

@ConnectorSettings("UNISWAP")
public class UniswapConnectorSettings extends DataConnectorSettings {

    private String subgraphUrl = "https://api.thegraph.com/subgraphs/name/uniswap/uniswap-v3";

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
}
