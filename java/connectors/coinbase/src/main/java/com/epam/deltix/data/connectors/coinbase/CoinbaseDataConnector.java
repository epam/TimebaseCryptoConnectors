package com.epam.deltix.data.connectors.coinbase;

import com.epam.deltix.data.connectors.commons.*;
import com.epam.deltix.data.connectors.commons.annotations.Connector;

@Connector("Coinbase")
public class CoinbaseDataConnector extends DataConnector<CoinbaseConnectorSettings> {
    private String wsUrl;

    public CoinbaseDataConnector(CoinbaseConnectorSettings settings) {
        super(settings, MdModel.availability()
                .withTrades()
                .withLevel1()
                .withLevel2().build()
        );

        this.wsUrl = settings.getWsUrl();
    }

    @Override
    protected RetriableFactory<MdFeed> doSubscribe(
            final MdModel.Options selected,
            final CloseableMessageOutputFactory outputFactory,
            final String... symbols) {

        return errorListener -> {
            final CoinbaseFeed result = new CoinbaseFeed(wsUrl,
                    selected,
                    outputFactory.create(),
                    errorListener,
                    symbols);
            result.start();
            return result;
        };
    }

    public static void main(String[] args) throws Exception {
        final CoinbaseDataConnector dataConnector = new CoinbaseDataConnector(
                new CoinbaseConnectorSettings(
                        "wss://ws-feed.pro.coinbase.com",
                        "dxtick://localhost:8011",
                        "coinbase"
                )
        );

        final MdModel model = dataConnector.model();

        final MdModel.Availability availability = model.available();
        System.out.println(availability);

        dataConnector.subscribe(
                model.select()
                        .withTrades()
                        .withLevel1()
                        .withLevel2()
                        .build(),
                "ETH-USD", "ETH-EUR"
        );

        System.in.read();

        dataConnector.close();

        System.out.println("CLOSED");
    }
}
