package com.epam.deltix.data.connectors.bybit;

import com.epam.deltix.data.connectors.commons.*;
import com.epam.deltix.data.connectors.commons.annotations.Connector;

@Connector("Bybit-Futures")
public class BybitFuturesDataConnector extends DataConnector<BybitFuturesConnectorSettings> {
    public BybitFuturesDataConnector(BybitFuturesConnectorSettings settings) {
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
            final BybitFuturesFeed result = new BybitFuturesFeed(
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
