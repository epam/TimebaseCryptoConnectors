package com.epam.deltix.data.connectors.bybit;

import com.epam.deltix.data.connectors.commons.*;
import com.epam.deltix.data.connectors.commons.annotations.Connector;

@Connector("Bybit")
public class BybitDataConnector extends DataConnector<BybitConnectorSettings> {
    public BybitDataConnector(BybitConnectorSettings settings) {
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
            final BybitFeed result = new BybitFeed(
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
