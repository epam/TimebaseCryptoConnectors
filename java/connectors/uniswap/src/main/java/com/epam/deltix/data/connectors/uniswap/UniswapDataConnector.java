package com.epam.deltix.data.connectors.uniswap;

import com.epam.deltix.data.connectors.commons.*;
import com.epam.deltix.data.connectors.commons.annotations.Connector;

@Connector("UNISWAP")
public class UniswapDataConnector extends DataConnector<UniswapConnectorSettings> {
    public UniswapDataConnector(final UniswapConnectorSettings settings) {
        super(settings, MdModel.availability()
                .withLevel2()
                .build()
        );
    }

    @Override
    protected RetriableFactory<MdFeed> doSubscribe(
            final MdModel.Options selected,
            final CloseableMessageOutputFactory outputFactory,
            final String... symbols) {
        return errorListener -> {

            final UniswapFeed result = new UniswapFeed(
                    settings().getNodeUrl(),
                    settings().getSubgraphUrl(),
                    selected,
                    outputFactory.create(),
                    errorListener,
                    logger(),
                    10_000,
                    settings().getAmount(),
                    settings().getDepth(),
                    symbols
            );
            result.start();
            return result;
        };
    }
}
