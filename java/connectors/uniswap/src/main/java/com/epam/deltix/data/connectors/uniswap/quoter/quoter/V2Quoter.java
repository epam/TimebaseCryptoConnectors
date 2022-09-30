package com.epam.deltix.data.connectors.uniswap.quoter.quoter;

import com.epam.deltix.data.connectors.uniswap.quoter.common.ContractService;
import com.epam.deltix.data.connectors.uniswap.quoter.common.ContractUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.Address;
import org.web3j.abi.datatypes.DynamicArray;
import org.web3j.abi.datatypes.Function;
import org.web3j.abi.datatypes.Type;
import org.web3j.abi.datatypes.generated.Uint256;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.http.HttpService;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.IntStream;

public class V2Quoter implements Quoter {
    final private Web3j w3client;
    final private String tokenBaseAddress;
    final private String tokenQuoteAddress;
    final private ContractService contractService;
    final private ContractUtils contractUtils;
    final private int tokenBaseDecimals;
    final private int tokenQuoteDecimals;

    final private String GET_AMOUNTS_IN = "getAmountsIn";
    final private String GET_AMOUNTS_OUT = "getAmountsOut";
    final private String V2_ROUTER_ADDRESS = "0xf164fC0Ec4E93095b804a4795bBe1e041497b92a";

    public V2Quoter(final String nodeUrl, final String tokenBaseAddress, final String tokenQuoteAddress) {
        this.tokenBaseAddress = tokenBaseAddress;
        this.tokenQuoteAddress = tokenQuoteAddress;
        this.w3client = Web3j.build(new HttpService(nodeUrl));
        this.contractService = new ContractService(w3client);
        this.contractUtils = new ContractUtils();
        tokenBaseDecimals = contractService.getTokenDecimals(tokenBaseAddress);
        tokenQuoteDecimals = contractService.getTokenDecimals(tokenQuoteAddress);
    }

    public String getTokenBaseAddress() {
        return tokenBaseAddress;
    }

    public String getTokenQuoteAddress() {
        return tokenQuoteAddress;
    }

    public JSONObject buildLevel2(final BigDecimal maxAmount, final int levelsQuantity) {
        JSONObject result = new JSONObject();

        result.put("timestamp", new Timestamp(System.currentTimeMillis()));

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

    @SuppressWarnings("unchecked")
    public BigDecimal getAsk(final BigDecimal amount) {
        BigDecimal result = new BigDecimal(0);

        DynamicArray<Address> path =
                new DynamicArray<>(new Address(tokenBaseAddress),
                        new Address(tokenQuoteAddress));

        Uint256 amountOut = contractUtils.bigDecimalToUint256(amount, tokenQuoteDecimals);

        Function getAmountsInFunction = new Function(
                GET_AMOUNTS_IN,
                Arrays.asList(amountOut, path),
                Collections.singletonList(new TypeReference<DynamicArray<Uint256>>() {
                }));

        List<Type> getAmountsInResult = contractService.callContract(getAmountsInFunction, V2_ROUTER_ADDRESS);

        if (getAmountsInResult.size() > 0) {
            List<Uint256> amountsInResultArray = (List<Uint256>) getAmountsInResult.get(0).getValue();
            Uint256 quote = amountsInResultArray.get(0);
            result = contractUtils.uint256ToBigDecimal(quote, tokenBaseDecimals);
        }

        return result;
    }

    @SuppressWarnings("unchecked")
    public BigDecimal getBid(final BigDecimal amount) {
        BigDecimal result = new BigDecimal(0);

        DynamicArray<Address> path =
                new DynamicArray<>(new Address(tokenQuoteAddress),
                        new Address(tokenBaseAddress));

        Uint256 amountIn = contractUtils.bigDecimalToUint256(amount, tokenQuoteDecimals);

        Function getAmountsInFunction = new Function(
                GET_AMOUNTS_OUT,
                Arrays.asList(amountIn, path),
                Collections.singletonList(new TypeReference<DynamicArray<Uint256>>() {
                }));

        List<Type> getAmountsInResult = contractService.callContract(getAmountsInFunction, V2_ROUTER_ADDRESS);

        if (getAmountsInResult.size() > 0) {
            List<Uint256> amountsInResultArray = (List<Uint256>) getAmountsInResult.get(0).getValue();
            Uint256 quote = amountsInResultArray.get(1);
            result = contractUtils.uint256ToBigDecimal(quote, tokenBaseDecimals);
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

        public V2Quoter build() {
            return new V2Quoter(nodeUrl, tokenBaseAddress, tokenQuoteAddress);
        }
    }
}
