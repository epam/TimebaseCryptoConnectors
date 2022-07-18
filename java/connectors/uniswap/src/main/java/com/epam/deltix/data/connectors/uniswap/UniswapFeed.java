package com.epam.deltix.data.connectors.uniswap;

import com.epam.deltix.data.connectors.commons.*;
import com.epam.deltix.data.connectors.uniswap.subscriptions.*;
import com.epam.deltix.data.uniswap.*;

import java.util.*;

public class UniswapFeed extends HttpFeed {
    private final String uri;
    private final int pollTimeoutMillis;
    private final String[] symbols;
    private Map<String, String> instrumentsMap = new HashMap<>();

    public UniswapFeed(
            final String uri,
            final MdModel.Options selected,
            final CloseableMessageOutput output,
            final ErrorListener errorListener,
            final Logger logger,
            final int pollTimeoutMillis,
            final String instruments,
            final String... symbols
    ) {
        super(selected, output, errorListener, logger);

        this.uri = uri;
        this.pollTimeoutMillis = pollTimeoutMillis;
        this.symbols = symbols != null ? symbols : new String[]{};
        Arrays.stream(Util.splitInstruments(instruments))
                .map(String::trim).forEach(e -> {
                    String[] item = e.split("=");
                    instrumentsMap.put(item[0], item[1]);
                });
    }

    @Override
    public void start() {
        super.start();

        // 1. resolve uniswap token ids
        IdentifiedUniswapSymbol[] identifiedUniswapSymbols;
        if (symbols == null || symbols.length == 0) {
            identifiedUniswapSymbols = new IdentifiedUniswapSymbol[]{};
        } else {
            try {
                if (instrumentsMap.size() > 0) {
                    identifiedUniswapSymbols = instrumentsMap.keySet().stream().map(e -> {
                        String[] ids = instrumentsMap.get(e).split("/");
                        return new IdentifiedUniswapSymbol(
                                new UniswapSymbol(e),
                                ids[0],
                                ids[1]
                        );
                    }).toArray(IdentifiedUniswapSymbol[]::new);
                } else {
                    identifiedUniswapSymbols = getIdentifiedUniswapSymbols();
                }
            } catch (final Throwable t) {
                onError(t);
                return; // cannot continue without ids.
                // The error event sent to be resolved (process to be re-scheduled)
            }
        }

        // 2. prepare and run pollers
        final MdModel.Options selected = selected();

        final List<Subscription> subscriptions = new ArrayList<>();

        if (selected.custom(FactoryAction.class)) {
            subscriptions.add(new FactorySubscription(
                    uri,
                    this,
                    logger(),
                    identifiedUniswapSymbols
            ));
        }
        if (selected.custom(BundleAction.class)) {
            subscriptions.add(new BundleSubscription(
                    uri,
                    this,
                    logger(),
                    identifiedUniswapSymbols
            ));
        }
        if (selected.custom(PoolAction.class)) {
            subscriptions.add(new PoolSubscription(
                    uri,
                    this,
                    logger(),
                    identifiedUniswapSymbols
            ));
        }
        if (selected.custom(TokenAction.class)) {
            subscriptions.add(new TokenSubscription(
                    uri,
                    this,
                    logger(),
                    identifiedUniswapSymbols
            ));
        }
        if (selected.custom(PositionAction.class)) {
            subscriptions.add(new PositionSubscription(
                    uri,
                    this,
                    logger(),
                    identifiedUniswapSymbols
            ));
        }

        if (selected.custom(TickAction.class)) {
            subscriptions.add(new TickSubscription(
                    uri,
                    this,
                    logger(),
                    identifiedUniswapSymbols
            ));
        }

        for (final Subscription subscription : subscriptions) {
            for (final HttpPoller poller : subscription.get()) {
                schedule(poller, pollTimeoutMillis);
            }
        }
    }

    private IdentifiedUniswapSymbol[] getIdentifiedUniswapSymbols() throws Exception {
        final UniswapSymbol[] uniswapSymbols = Arrays.stream(symbols)
                .map(UniswapSymbol::new).toArray(UniswapSymbol[]::new);

        final String[] tokenSymbols =
                UniswapSymbol.collectSymbols(uniswapSymbols);

        final Map<String, TokenIdentifier.Info> tokenInfos =
                TokenIdentifier.identify(uri, tokenSymbols);

        return IdentifiedUniswapSymbol.identify(tokenInfos, uniswapSymbols);
    }
}
