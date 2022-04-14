package com.epam.deltix.data.connectors.kraken;

import com.epam.deltix.data.connectors.commons.*;
import com.epam.deltix.data.connectors.commons.annotations.Connector;

@Connector("Kraken-Spot")
public class KrakenSpotDataConnector extends DataConnector<KrakenSpotConnectorSettings> {
    private String wsUrl;
    private int depth;

    public KrakenSpotDataConnector(KrakenSpotConnectorSettings settings) {
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
            final KrakenSpotFeed result = new KrakenSpotFeed(wsUrl, depth,
                selected,
                outputFactory.create(),
                errorListener,
                symbols);
            result.start();
            return result;
        };
    }

}
