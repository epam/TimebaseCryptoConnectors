package com.epam.deltix.data.connectors.uniswap;

import com.epam.deltix.data.connectors.commons.DataConnectorSettings;
import com.epam.deltix.data.connectors.commons.annotations.ConnectorSettings;

@ConnectorSettings("UNISWAP")
public class UniswapConnectorSettings extends DataConnectorSettings {

    private String subgraphUrl = "https://api.thegraph.com/subgraphs/name/ianlapham/uniswap-v3-subgraph";
    String instruments;

    public UniswapConnectorSettings() {
        super();
    }

    public UniswapConnectorSettings(final String name, final String tbUrl, final String stream,
                                    final String instruments) {
        super(tbUrl, stream);
        setName(name);
        setSubgraphUrl(subgraphUrl);
        setInstruments(instruments);
    }

    public String getSubgraphUrl() {
        return subgraphUrl;
    }

    public void setSubgraphUrl(String subgraphUrl) {
        this.subgraphUrl = subgraphUrl;
    }

    public String getInstruments() {
        return instruments;
    }

    public void setInstruments(String instruments) {
        this.instruments = instruments;
    }
}
