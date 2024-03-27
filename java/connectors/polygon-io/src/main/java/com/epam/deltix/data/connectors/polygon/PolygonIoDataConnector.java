package com.epam.deltix.data.connectors.polygon;

import com.epam.deltix.data.connectors.commons.*;
import com.epam.deltix.data.connectors.commons.annotations.Connector;

@Connector("POLYGON-IO")
public class PolygonIoDataConnector extends DataConnector<PolygonIoConnectorSettings> {

    private final PolygonIoEndpoint endpoint;

    public PolygonIoDataConnector(PolygonIoConnectorSettings settings) {
        super(settings, availabilityModel(settings));
        endpoint = PolygonIoEndpoint.typeFromAddress(settings.getWsUrl());
    }

    private static MdModel availabilityModel(PolygonIoConnectorSettings settings) {
        PolygonIoEndpoint endpoint = PolygonIoEndpoint.typeFromAddress(settings.getWsUrl());
        if (endpoint == null) {
            throw new RuntimeException("Invalid wsUrl: " + settings.getWsUrl());
        }

        if (endpoint == PolygonIoEndpoint.Crypto) {
            return MdModel.availability()
                .withTrades()
                .withLevel1()
                .withLevel2()
                .build();
        }

        if (endpoint == PolygonIoEndpoint.Forex || endpoint == PolygonIoEndpoint.Indices) {
            return MdModel.availability()
                .withLevel1()
                .build();
        }

        return MdModel.availability()
            .withTrades()
            .withLevel1()
            .build();
    }

    @Override
    protected RetriableFactory<MdFeed> doSubscribe(
            final MdModel.Options selected,
            final CloseableMessageOutputFactory outputFactory,
            final String... symbols) {

        if (endpoint == PolygonIoEndpoint.Stocks || endpoint == PolygonIoEndpoint.Options) {
            return errorListener -> {
                final PolygonIoFeed result = new PolygonIoStocksFeed(
                    settings(),
                    selected,
                    outputFactory.create(),
                    errorListener,
                    logger(),
                    symbols);
                result.start();
                return result;
            };
        } else if (endpoint == PolygonIoEndpoint.Forex) {
            return errorListener -> {
                final PolygonIoFeed result = new PolygonIoStocksFeed(
                    settings(),
                    selected,
                    outputFactory.create(),
                    errorListener,
                    logger(),
                    symbols);
                result.start();
                return result;
            };
        } else if (endpoint == PolygonIoEndpoint.Crypto) {
            return errorListener -> {
                final PolygonIoFeed result = new PolygonIoCryptoFeed(
                    settings(),
                    selected,
                    outputFactory.create(),
                    errorListener,
                    logger(),
                    symbols);
                result.start();
                return result;
            };
        } else if (endpoint == PolygonIoEndpoint.Indices) {
            return errorListener -> {
                final PolygonIoFeed result = new PolygonIoIndicesFeed(
                    settings(),
                    selected,
                    outputFactory.create(),
                    errorListener,
                    logger(),
                    symbols);
                result.start();
                return result;
            };
        }

        throw new RuntimeException("Unsupported endpoint: " + endpoint);
    }

}
