package com.epam.deltix.data.connectors.kraken;

import com.epam.deltix.data.connectors.commons.*;
import com.epam.deltix.data.connectors.commons.annotations.Connector;

@Connector("Kraken-Futures")
public class KrakenFuturesDataConnector extends DataConnector<KrakenFuturesConnectorSettings> {
    public KrakenFuturesDataConnector(KrakenFuturesConnectorSettings settings) {
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
            final KrakenFuturesFeed result = new KrakenFuturesFeed(
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
