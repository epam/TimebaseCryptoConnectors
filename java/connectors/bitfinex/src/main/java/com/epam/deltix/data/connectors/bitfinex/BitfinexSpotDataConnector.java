package com.epam.deltix.data.connectors.bitfinex;

import com.epam.deltix.data.connectors.commons.*;
import com.epam.deltix.data.connectors.commons.annotations.Connector;

@Connector("BITFINEX")
public class BitfinexSpotDataConnector extends DataConnector<BitfinexSpotConnectorSettings> {
    private String wsUrl;
    private int depth;

    public BitfinexSpotDataConnector(BitfinexSpotConnectorSettings settings) {
        super(settings, MdModel.availability()
            .withTrades()
            .withLevel1()
            .withLevel2().build()
        );

        this.wsUrl = settings.getWsUrl();
        this.depth = settings.getDepth();
    }

    @Override
    protected RetriableFactory<MdFeed> doSubscribe(
            final MdModel.Options selected,
            final CloseableMessageOutputFactory outputFactory,
            final String... symbols) {

        return errorListener -> {
            final BitfinexSpotFeed result = new BitfinexSpotFeed(wsUrl, depth,
                selected,
                outputFactory.create(),
                errorListener,
                symbols);
            result.start();
            return result;
        };
    }

}
