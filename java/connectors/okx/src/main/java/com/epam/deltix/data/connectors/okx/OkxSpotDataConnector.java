package com.epam.deltix.data.connectors.okx;

import com.epam.deltix.data.connectors.commons.*;
import com.epam.deltix.data.connectors.commons.annotations.Connector;

@Connector("OKX")
public class OkxSpotDataConnector extends DataConnector<OkxSpotConnectorSettings> {
    public OkxSpotDataConnector(OkxSpotConnectorSettings settings) {
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
            final OkxSpotFeed result = new OkxSpotFeed(
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
