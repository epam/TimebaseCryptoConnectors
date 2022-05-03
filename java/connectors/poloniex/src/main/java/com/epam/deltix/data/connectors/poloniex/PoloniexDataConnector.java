package com.epam.deltix.data.connectors.poloniex;

import com.epam.deltix.data.connectors.commons.*;
import com.epam.deltix.data.connectors.commons.annotations.Connector;

@Connector("Poloniex")
public class PoloniexDataConnector extends DataConnector<PoloniexConnectorSettings> {
    public PoloniexDataConnector(PoloniexConnectorSettings settings) {
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
            final PoloniexFeed result = new PoloniexFeed(
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
