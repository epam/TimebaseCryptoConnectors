package com.epam.deltix.data.connectors.uniswap;

import com.epam.deltix.data.connectors.commons.CloseableMessageOutputFactory;
import com.epam.deltix.data.connectors.commons.DataConnector;
import com.epam.deltix.data.connectors.commons.MdFeed;
import com.epam.deltix.data.connectors.commons.MdModel;
import com.epam.deltix.data.connectors.commons.RetriableFactory;
import com.epam.deltix.data.connectors.commons.annotations.Connector;
import com.epam.deltix.data.uniswap.PoolAction;
import com.epam.deltix.data.uniswap.TokenAction;

@Connector("UNISWAP")
public class UniswapDataConnector extends DataConnector<UniswapConnectorSettings> {
    public UniswapDataConnector(final UniswapConnectorSettings settings) {
        super(settings, MdModel.availability()
            .withCustom(
                    PoolAction.class,
                    TokenAction.class)
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
                    settings().getSubgraphUrl(),
                    selected,
                    outputFactory.create(),
                    errorListener,
                    logger(),
                    5_000
                    );
            result.start();
            return result;
        };
    }

    public static void main(String[] args) throws Exception {
        final UniswapDataConnector dataConnector = new UniswapDataConnector(
                new UniswapConnectorSettings(
                        "uniswap",
                        "dxtick://localhost:8011",
                        "uniswap"
                )
        );

        final MdModel model = dataConnector.model();

        final MdModel.Availability availability = model.available();
        dataConnector.logger().info(() -> availability.toString());

        dataConnector.subscribe(
                model.select().
                        withCustom(PoolAction.class, TokenAction.class).
                        build()
        );

        System.in.read();

        dataConnector.close();

        dataConnector.logger().info(() -> "CLOSED");
    }
}
