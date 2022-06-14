package com.epam.deltix.data.connectors.ascendex;

import com.epam.deltix.data.connectors.commons.*;
import com.epam.deltix.data.connectors.commons.annotations.Connector;

@Connector("Ascendex")
public class AscendexDataConnector extends DataConnector<AscendexConnectorSettings> {
    public AscendexDataConnector(AscendexConnectorSettings settings) {
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
            final AscendexFeed result = new AscendexFeed(
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
