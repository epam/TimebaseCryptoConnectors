package com.epam.deltix.data.connectors.kucoin;

import com.epam.deltix.data.connectors.commons.*;
import com.epam.deltix.data.connectors.commons.annotations.Connector;

@Connector("Kucoin")
public class KucoinDataConnector extends DataConnector<KucoinConnectorSettings> {
    public KucoinDataConnector(KucoinConnectorSettings settings) {
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
            final KucoinFeed result = new KucoinFeed(
                    settings().getWsUrl(),
                    settings().getRestUrl(),
                    settings().getDepth(),
                    selected,
                    outputFactory.create(),
                    errorListener,
                    logger(),
                    true,
                    symbols);
            result.start();
            return result;
        };
    }
}
