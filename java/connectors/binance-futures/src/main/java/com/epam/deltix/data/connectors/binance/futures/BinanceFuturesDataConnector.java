package com.epam.deltix.data.connectors.binance.futures;

import com.epam.deltix.data.connectors.commons.*;
import com.epam.deltix.data.connectors.commons.annotations.Connector;

@Connector("Binance-Futures")
public class BinanceFuturesDataConnector extends DataConnector<BinanceFuturesConnectorSettings> {
    public BinanceFuturesDataConnector(BinanceFuturesConnectorSettings settings) {
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
            final BinanceFuturesFeed result = new BinanceFuturesFeed(
                    settings().getWsUrl(),
                    settings().getRestUrl(),
                    settings().getDepth(),
                    selected,
                    outputFactory.create(),
                    errorListener,
                    logger(),
                    false,
                    symbols);
            result.start();
            return result;
        };
    }
}
