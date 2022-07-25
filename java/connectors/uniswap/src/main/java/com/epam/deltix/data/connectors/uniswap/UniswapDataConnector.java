package com.epam.deltix.data.connectors.uniswap;

import com.epam.deltix.data.connectors.commons.*;
import com.epam.deltix.data.connectors.commons.annotations.Connector;
import com.epam.deltix.data.uniswap.*;
import com.epam.deltix.timebase.messages.InstrumentMessage;

@Connector("UNISWAP")
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
                    settings().getInstruments(),
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
                        "uniswap",
                        "BUSD/WETH" + "=" + "0x4fabb145d64652a948d72533023f6e7a623c7c53" + "/" + "0xc02aaa39b223fe8d0a0e5c4f27ead9083c756cc2"
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
