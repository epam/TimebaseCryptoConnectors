package com.epam.deltix.data.connectors.uniswapsubgraph;

import com.epam.deltix.data.connectors.commons.*;
import com.epam.deltix.data.connectors.commons.annotations.Connector;
import com.epam.deltix.data.uniswap.*;
import com.epam.deltix.timebase.messages.InstrumentMessage;

@Connector("UNISWAP-SUBGRAPH")
public class UniswapDataConnector extends DataConnector<UniswapConnectorSettings> {
    public UniswapDataConnector(final UniswapConnectorSettings settings) {
        super(settings, MdModel.availability()
                .withCustom(
                        FactoryAction.class,
                        BundleAction.class,
                        PoolAction.class,
                        TokenAction.class,
                        PositionAction.class,
                        TickAction.class,
                        SwapAction.class,
                        MintAction.class,
                        BurnAction.class,
                        CollectAction.class,
                        FlashAction.class,
                        TransactionAction.class,
                        PositionSnapshotAction.class,
                        UniswapDayDataAction.class,
                        TokenDayDataAction.class,
                        TokenHourDataAction.class)
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
                    settings().getSubgraphUrl(),
                    selected,
                    outputFactory.create(),
                    errorListener,
                    logger(),
                    10_000,
                    settings().getAmount(),
                    settings().getDepth(),
                    settings().getUniswapApiUrl(),
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
                                TokenAction.class,
                                PositionAction.class,
                                TickAction.class).
                        build(),
                "BUSD/WETH"
        );

        System.in.read();

        dataConnector.close();

        dataConnector.logger().info(() -> "CLOSED");
    }
}
