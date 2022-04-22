package com.epam.deltix.data.connectors.bitfinex;

import com.epam.deltix.data.connectors.commons.*;
import com.epam.deltix.data.connectors.commons.annotations.Connector;

@Connector("BITFINEX")
public class BitfinexSpotDataConnector extends DataConnector<BitfinexSpotConnectorSettings> {
    public BitfinexSpotDataConnector(BitfinexSpotConnectorSettings settings) {
        super(settings, MdModel.availability()
            .withTrades()
            .withLevel1()
            .withLevel2().build()
        );
    }

    @Override
    protected RetriableFactory<MdFeed> doSubscribe(
            final MdModel.Options selected,
            final CloseableMessageOutputFactory outputFactory,
            final String... symbols) {

        return errorListener -> {
            final BitfinexSpotFeed result = new BitfinexSpotFeed(
                settings().getWsUrl(),
                settings().getDepth(),
                selected,
                outputFactory.create(),
                errorListener,
                symbols);
            result.start();
            return result;
        };
    }

}
