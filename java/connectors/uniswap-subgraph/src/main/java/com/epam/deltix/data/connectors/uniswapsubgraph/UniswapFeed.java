package com.epam.deltix.data.connectors.uniswapsubgraph;

import com.epam.deltix.data.connectors.commons.*;
import com.epam.deltix.data.connectors.uniswapsubgraph.subscriptions.*;
import com.epam.deltix.data.uniswap.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class UniswapFeed extends HttpFeed {
    private final String uri;
    private final int pollTimeoutMillis;
    private final String[] symbols;
    private final String uniswapApiUrl;
    private final int amount;
    private final int depth;

    public UniswapFeed(
            final String uri,
            final MdModel.Options selected,
            final CloseableMessageOutput output,
            final ErrorListener errorListener,
            final Logger logger,
            final int pollTimeoutMillis,
            final int amount,
            final int depth,
            final String uniswapApiUrl,
            final String... symbols
    ) {
        super(selected, output, errorListener, logger);

        this.uri = uri;
        this.pollTimeoutMillis = pollTimeoutMillis;
        this.symbols = symbols != null ? symbols : new String[]{};
        this.uniswapApiUrl = uniswapApiUrl;
        this.amount = amount;
        this.depth = depth;
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
                identifiedUniswapSymbols = getIdentifiedUniswapSymbols();
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

        if (selected.custom(SwapAction.class)) {
            subscriptions.add(new SwapSubscription(
                    uri,
                    this,
                    logger(),
                    identifiedUniswapSymbols
            ));
        }

        if (selected.custom(MintAction.class)) {
            subscriptions.add(new MintSubscription(
                    uri,
                    this,
                    logger(),
                    identifiedUniswapSymbols
            ));
        }

        if (selected.custom(BurnAction.class)) {
            subscriptions.add(new BurnSubscription(
                    uri,
                    this,
                    logger(),
                    identifiedUniswapSymbols
            ));
        }

        if (selected.level2()) {
            subscriptions.add(new PriceSubscription(
                    uniswapApiUrl,
                    this,
                    logger(),
                    selected,
                    amount,
                    depth,
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
