package com.epam.deltix.data.connectors.uniswap.quoter.quoter;

import com.epam.deltix.data.connectors.uniswap.quoter.common.ContractService;
import com.epam.deltix.data.connectors.uniswap.quoter.common.ContractUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.Address;
import org.web3j.abi.datatypes.Function;
import org.web3j.abi.datatypes.Type;
import org.web3j.abi.datatypes.generated.Uint160;
import org.web3j.abi.datatypes.generated.Uint24;
import org.web3j.abi.datatypes.generated.Uint256;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.http.HttpService;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.sql.Timestamp;
import java.util.*;
import java.util.stream.IntStream;

public class V3Quoter implements Quoter {
    final private Web3j w3client;
    final private String tokenBaseAddress;
    final private String tokenQuoteAddress;
    final private ContractService contractService;
    final private ContractUtils contractUtils;
    final private int tokenBaseDecimals;
    final private int tokenQuoteDecimals;

    final private String QUOTE_EXACT_OUTPUT_SINGLE = "quoteExactOutputSingle";
    final private String QUOTE_EXACT_INPUT_SINGLE = "quoteExactInputSingle";
    final private String V3_QUOTER = "0xb27308f9F90D607463bb33eA1BeBb41C27CE5AB6";

    private static final Set<Integer> UNISWAP_POOL_FEES = Set.of(
            500, 3000, 10000
    );

    public V3Quoter(String nodeUrl, String tokenBaseAddress, String tokenQuoteAddress) {
        this.tokenBaseAddress = tokenBaseAddress;
        this.tokenQuoteAddress = tokenQuoteAddress;
        this.w3client = Web3j.build(new HttpService(nodeUrl));
        this.contractService = new ContractService(w3client);
        this.contractUtils = new ContractUtils();
        this.tokenBaseDecimals = contractService.getTokenDecimals(tokenBaseAddress);
        this.tokenQuoteDecimals = contractService.getTokenDecimals(tokenQuoteAddress);
    }

    public String getTokenBaseAddress() {
        return tokenBaseAddress;
    }

    public String getTokenQuoteAddress() {
        return tokenQuoteAddress;
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

    public BigDecimal getAsk(final BigDecimal amount) {
        Optional<BigDecimal> quote = UNISWAP_POOL_FEES.stream()
                .map(fee -> fetchAsk(amount, fee))
                .filter(item -> item.compareTo(new BigDecimal(0)) != 0)
                .min(BigDecimal::compareTo);

        return quote.isEmpty() ? new BigDecimal(0) : quote.get();
    }

    private BigDecimal fetchAsk(BigDecimal amount, int fee) {
        BigDecimal result = new BigDecimal(0);

        Uint256 amountOut = contractUtils.bigDecimalToUint256(amount, tokenQuoteDecimals);
        Uint24 feeUint = new Uint24(fee);
        Uint160 sqrtPriceLimitX96 = new Uint160(0);

        Function getAmountsInFunction = new Function(
                QUOTE_EXACT_OUTPUT_SINGLE,
                Arrays.asList(new Address(tokenBaseAddress), new Address(tokenQuoteAddress), feeUint, amountOut, sqrtPriceLimitX96),
                Collections.singletonList(new TypeReference<Uint256>() {
                }));

        List<Type> getAmountsInResult = contractService.callContract(getAmountsInFunction, V3_QUOTER);

        if (getAmountsInResult.size() > 0) {
            BigInteger quote = (BigInteger) getAmountsInResult.get(0).getValue();
            result = contractUtils.bigIntToBigDec(quote, tokenBaseDecimals);
        }
        return result;
    }

    public BigDecimal getBid(final BigDecimal amount) {
        Optional<BigDecimal> quote = UNISWAP_POOL_FEES.stream()
                .map(fee -> fetchBid(amount, fee))
                .filter(item -> item.compareTo(new BigDecimal(0)) != 0)
                .min(BigDecimal::compareTo);

        return quote.isEmpty() ? new BigDecimal(0) : quote.get();
    }

    private BigDecimal fetchBid(BigDecimal amount, int fee) {
        BigDecimal result = new BigDecimal(0);

        Uint256 amountIn = contractUtils.bigDecimalToUint256(amount, tokenQuoteDecimals);
        Uint24 feeUint = new Uint24(fee);
        Uint160 sqrtPriceLimitX96 = new Uint160(0);

        Function getAmountsInFunction = new Function(
                QUOTE_EXACT_INPUT_SINGLE,
                Arrays.asList(new Address(tokenQuoteAddress), new Address(tokenBaseAddress), feeUint, amountIn, sqrtPriceLimitX96),
                Collections.singletonList(new TypeReference<Uint256>() {
                }));

        List<Type> getAmountsInResult = contractService.callContract(getAmountsInFunction, V3_QUOTER);

        if (getAmountsInResult.size() > 0) {
            BigInteger quote = (BigInteger) getAmountsInResult.get(0).getValue();
            result = contractUtils.bigIntToBigDec(quote, tokenBaseDecimals);
        }
        return result;
    }

    public static class Builder {
        private String nodeUrl;
        private String tokenBaseAddress;
        private String tokenQuoteAddress;

        public Builder setNodeUrl(String nodeUrl) {
            this.nodeUrl = nodeUrl;

            return this;
        }

        public Builder setTokenBaseAddress(String tokenBaseAddress) {
            this.tokenBaseAddress = tokenBaseAddress;

            return this;
        }

        public Builder setTokenQuoteAddress(String tokenQuoteAddress) {
            this.tokenQuoteAddress = tokenQuoteAddress;

            return this;
        }

        public V3Quoter build() {
            return new V3Quoter(nodeUrl, tokenBaseAddress, tokenQuoteAddress);
        }
    }
}
