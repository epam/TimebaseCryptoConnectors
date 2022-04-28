package com.epam.deltix.data.connectors.hitbtc;

import com.epam.deltix.data.connectors.commons.*;
import com.epam.deltix.data.connectors.commons.annotations.Connector;

@Connector("HITBTC")
public class HitbtcDataConnector extends DataConnector<HitbtcConnectorSettings> {
    public HitbtcDataConnector(HitbtcConnectorSettings settings) {
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
            final HitbtcFeed result = new HitbtcFeed(
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
