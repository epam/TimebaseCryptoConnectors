package com.epam.deltix.data.connectors.binance.spot;

import com.epam.deltix.data.connectors.commons.*;
import com.epam.deltix.data.connectors.commons.annotations.Connector;

@Connector("Binance-Spot")
public class BinanceSpotDataConnector extends DataConnector<BinanceSpotConnectorSettings> {
    public BinanceSpotDataConnector(BinanceSpotConnectorSettings settings) {
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
            final BinanceSpotFeed result = new BinanceSpotFeed(
                    settings().getWsUrl(),
                    settings().getRestUrl(),
                    settings().getDepth(),
                    selected,
                    outputFactory.create(),
                    errorListener,
                    logger(),
                    symbols);
            result.start();
            return result;
        };
    }
}
