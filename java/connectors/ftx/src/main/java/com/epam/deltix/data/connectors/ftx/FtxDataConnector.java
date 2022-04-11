package com.epam.deltix.data.connectors.ftx;

import com.epam.deltix.data.connectors.commons.*;
import com.epam.deltix.data.connectors.commons.annotations.Connector;

@Connector("FTX")
public class FtxDataConnector extends DataConnector<FtxConnectorSettings> {
    private String wsUrl;
    private int depth;

    public FtxDataConnector(FtxConnectorSettings settings) {
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
            final FtxFeed result = new FtxFeed(wsUrl, depth,
                selected,
                outputFactory.create(),
                errorListener,
                symbols);
            result.start();
            return result;
        };
    }

}
