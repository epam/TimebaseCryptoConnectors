package com.epam.deltix.data.connectors.onetrading;

import com.epam.deltix.data.connectors.commons.*;
import com.epam.deltix.data.connectors.commons.annotations.Connector;

@Connector("ONETRADING")
public class OneTradingSpotDataConnector extends DataConnector<OneTradingSpotConnectorSettings> {
    public OneTradingSpotDataConnector(OneTradingSpotConnectorSettings settings) {
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
            final OneTradingSpotFeed result = new OneTradingSpotFeed(
                    settings().getWsUrl(),
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
