package com.epam.deltix.data.connectors.uniswap.quoter.aggregator;


import com.epam.deltix.data.connectors.uniswap.quoter.quoter.Quoter;
import org.json.JSONArray;
import org.json.JSONObject;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class QuoteAggregator implements Aggregator {
    final private String tokenBaseAddress;
    final private String tokenQuoteAddress;
    final private List<Quoter> quoterList;

    public QuoteAggregator(final String tokenBaseAddress,
                           final String tokenQuoteAddress, final List<Quoter> quoterList) {
        this.tokenBaseAddress = tokenBaseAddress;
        this.tokenQuoteAddress = tokenQuoteAddress;

        this.quoterList = quoterList;
    }

    public String getTokenBaseAddress() {
        return tokenBaseAddress;
    }

    public String getTokenQuoteAddress() {
        return tokenQuoteAddress;
    }

    public List<Quoter> getQuoterList() {
        return quoterList;
    }

    public JSONObject buildLevel2(final BigDecimal maxAmount, final int levelsQuantity) {
        JSONObject result = new JSONObject();

        Date now = new Date();
        result.put("timestamp", new Timestamp(now.getTime()));

        JSONArray asks = new JSONArray();
        result.put("asks", asks);

        JSONArray bids = new JSONArray();
        result.put("bids", bids);

        BigDecimal levelsQuantityBigDec = new BigDecimal(levelsQuantity);

        BigDecimal step = maxAmount.divide(levelsQuantityBigDec);
        List<BigDecimal> sizeDistribution = new ArrayList<>();

        IntStream.rangeClosed(1, levelsQuantity).forEach(level -> {
            BigDecimal levelBigDec = new BigDecimal(level);
            sizeDistribution.add(step.multiply(levelBigDec));
        });

        sizeDistribution.stream().forEach(size -> {
            BigDecimal ask = getAsk(size);
            if (ask.compareTo(new BigDecimal(0)) == 1) {
                BigDecimal price = ask.divide(size, RoundingMode.HALF_UP);

                JSONArray askEntry = new JSONArray();
                askEntry.put(price.toString());
                askEntry.put(size.toString());

                asks.put(askEntry);
            }

            BigDecimal bid = getBid(size);
            if (bid.compareTo(new BigDecimal(0)) == 1) {
                BigDecimal price = bid.divide(size, RoundingMode.HALF_UP);

                JSONArray bidEntry = new JSONArray();
                bidEntry.put(price.toString());
                bidEntry.put(size.toString());

                bids.put(bidEntry);
            }
        });

        return result;
    }

    @Override
    public BigDecimal getAsk(final BigDecimal amount) {
        Optional<BigDecimal> bestQuote = quoterList.stream()
                .map(quoter -> quoter.getAsk(amount))
                .filter(e -> e.compareTo(new BigDecimal(0)) != 0)
                .min(BigDecimal::compareTo);

        return bestQuote.isEmpty() ? new BigDecimal(0) : bestQuote.get();
    }

    @Override
    public BigDecimal getBid(final BigDecimal amount) {
        Optional<BigDecimal> bestQuote = quoterList.stream()
                .map(quoter -> quoter.getBid(amount))
                .filter(e -> e.compareTo(new BigDecimal(0)) != 0)
                .min(BigDecimal::compareTo);

        return bestQuote.isEmpty() ? new BigDecimal(0) : bestQuote.get();
    }

    public static class Builder {
        private String nodeUrl;
        private String tokenBaseAddress;
        private String tokenQuoteAddress;
        private List<Protocol> protocols = new ArrayList<>();
        private List<Quoter> quoterList;

        public Builder setNodeUrl(final String nodeUrl) {
            this.nodeUrl = nodeUrl;

            return this;
        }

        public Builder setTokenBaseAddress(final String tokenBaseAddress) {
            this.tokenBaseAddress = tokenBaseAddress;

            return this;
        }

        public Builder setTokenQuoteAddress(final String tokenQuoteAddress) {
            this.tokenQuoteAddress = tokenQuoteAddress;

            return this;
        }

        public Builder withProtocol(final String protocol) {
            List<String> availableProtocols = Stream.of(Protocol.values()).map(
                    Protocol::name).collect(Collectors.toList());

            if (availableProtocols.contains(protocol.toUpperCase())) {
                this.protocols.add(Protocol.valueOf(protocol.toUpperCase()));
            }

            return this;
        }

        public QuoteAggregator build() {
            quoterList = protocols.stream()
                    .map(protocol -> QuoterFactory.buildQuoter(protocol, nodeUrl, tokenBaseAddress, tokenQuoteAddress))
                    .collect(Collectors.toList());

            return new QuoteAggregator(tokenBaseAddress, tokenQuoteAddress, quoterList);
        }
    }
}
