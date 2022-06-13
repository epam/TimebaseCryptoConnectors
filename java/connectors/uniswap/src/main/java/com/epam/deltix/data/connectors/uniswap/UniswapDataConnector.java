package com.epam.deltix.data.connectors.uniswap;

import com.epam.deltix.data.connectors.commons.CloseableMessageOutput;
import com.epam.deltix.data.connectors.commons.CloseableMessageOutputFactory;
import com.epam.deltix.data.connectors.commons.DataConnector;
import com.epam.deltix.data.connectors.commons.MdFeed;
import com.epam.deltix.data.connectors.commons.MdModel;
import com.epam.deltix.data.connectors.commons.RetriableFactory;
import com.epam.deltix.data.connectors.commons.annotations.Connector;
import com.epam.deltix.data.uniswap.BundleAction;
import com.epam.deltix.data.uniswap.Factory;
import com.epam.deltix.data.uniswap.FactoryAction;
import com.epam.deltix.data.uniswap.PoolAction;
import com.epam.deltix.data.uniswap.TokenAction;
import com.epam.deltix.timebase.messages.InstrumentMessage;

@Connector("UNISWAP")
public class UniswapDataConnector extends DataConnector<UniswapConnectorSettings> {
    public UniswapDataConnector(final UniswapConnectorSettings settings) {
        super(settings, MdModel.availability()
            .withCustom(
                    FactoryAction.class,
                    BundleAction.class,
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
                    5_000,
                    symbols
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

        DataConnector.DEBUG_OUTPUT_FACTORY = () -> new CloseableMessageOutput() {
            @Override
            public void close() {
                System.out.println("Close the message output");
            }

            @Override
            public void send(final InstrumentMessage message) {
                System.out.println(message);
            }
        };

        final MdModel model = dataConnector.model();

        final MdModel.Availability availability = model.available();
        dataConnector.logger().info(() -> availability.toString());

        dataConnector.subscribe(
                model.select().
                        withCustom(
                                FactoryAction.class,
                                BundleAction.class,
                                PoolAction.class,
                                TokenAction.class).
                        build(),
                "BUSD/WETH", "/ICHI"
        );

        System.in.read();

        dataConnector.close();

        dataConnector.logger().info(() -> "CLOSED");
    }
}
