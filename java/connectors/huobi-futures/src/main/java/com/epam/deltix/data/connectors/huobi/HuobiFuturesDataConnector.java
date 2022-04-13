package com.epam.deltix.data.connectors.huobi;

import com.epam.deltix.data.connectors.commons.*;
import com.epam.deltix.data.connectors.commons.annotations.Connector;

@Connector("Huobi-Futures")
public class HuobiFuturesDataConnector extends DataConnector<HuobiFuturesConnectorSettings> {
    private String wsUrl;
    private int depth;

    public HuobiFuturesDataConnector(HuobiFuturesConnectorSettings settings) {
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
            final HuobiFuturesFeed result = new HuobiFuturesFeed(wsUrl, depth,
                selected,
                outputFactory.create(),
                errorListener,
                symbols);
            result.start();
            return result;
        };
    }

}
