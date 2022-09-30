package com.epam.deltix.data.connectors.uniswap;

import com.epam.deltix.data.connectors.commons.*;
import com.epam.deltix.data.connectors.uniswap.quoter.aggregator.QuoteAggregator;
import com.epam.deltix.data.connectors.uniswap.quoter.quoter.V2Quoter;
import com.epam.deltix.data.connectors.uniswap.quoter.quoter.V3Quoter;
import com.epam.deltix.data.connectors.uniswap.subscriptions.IdentifiedUniswapSymbol;
import com.epam.deltix.data.connectors.uniswap.subscriptions.TokenIdentifier;
import com.epam.deltix.data.connectors.uniswap.subscriptions.UniswapSymbol;
import org.json.JSONObject;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Map;

public class UniswapFeed extends HttpFeed {
    private final String nodeUri;
    private final String subgraphUri;
    private final int pollTimeoutMillis;
    private final String[] symbols;
    private final int amount;
    private final int depth;

    public UniswapFeed(
            final String nodeUri,
            final String subgraphUri,
            final MdModel.Options selected,
            final CloseableMessageOutput output,
            final ErrorListener errorListener,
            final Logger logger,
            final int pollTimeoutMillis,
            final int amount,
            final int depth,
            final String... symbols
    ) {
        super(selected, output, errorListener, logger);

        this.nodeUri = nodeUri;
        this.subgraphUri = subgraphUri;
        this.pollTimeoutMillis = pollTimeoutMillis;
        this.symbols = symbols != null ? symbols : new String[]{};
        this.amount = amount;
        this.depth = depth;
    }

    @Override
    public void start() {
        super.start();

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

        Arrays.stream(identifiedUniswapSymbols).forEach(symbol -> {
            String name = symbol.token0() + "/" + symbol.token1();

            QuoteAggregator aggregator = new QuoteAggregator.Builder()
                    .setNodeUrl(nodeUri)
                    .setTokenBaseAddress(symbol.token0Id())
                    .setTokenQuoteAddress(symbol.token1Id())
                    .withProtocol("v2")
                    .withProtocol("v3")
                    .build();

            schedule(new UniswapPricePoller(
                    this,
                    selected(),
                    amount,
                    depth,
                    logger(),
                    name,
                    aggregator), pollTimeoutMillis);
        });
    }

    private IdentifiedUniswapSymbol[] getIdentifiedUniswapSymbols() throws Exception {
        final UniswapSymbol[] uniswapSymbols = Arrays.stream(symbols)
                .map(UniswapSymbol::new).toArray(UniswapSymbol[]::new);

        final String[] tokenSymbols =
                UniswapSymbol.collectSymbols(uniswapSymbols);

        final Map<String, TokenIdentifier.Info> tokenInfos =
                TokenIdentifier.identify(subgraphUri, tokenSymbols);

        return IdentifiedUniswapSymbol.identify(tokenInfos, uniswapSymbols);
    }

}
