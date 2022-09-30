package com.epam.deltix.data.connectors.uniswap.quoter.common;

import org.web3j.abi.datatypes.generated.Uint256;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;

public class ContractUtils {

    public Uint256 stringToUint256(final String amount, final int tokenDecimals) {
        BigDecimal bigDecimalAmount = new BigDecimal(amount);

        BigInteger bigIntegerAmount = bigDecimalAmount
                .multiply(new BigDecimal(Math.pow(10, tokenDecimals)))
                .toBigInteger();

        return new Uint256(bigIntegerAmount);
    }

    public Uint256 bigDecimalToUint256(final BigDecimal amount, final int tokenDecimals) {
        BigInteger bigIntegerAmount = amount
                .multiply(new BigDecimal(Math.pow(10, tokenDecimals)))
                .toBigInteger();

        return new Uint256(bigIntegerAmount);
    }

    public String uint256ToString(final Uint256 amount, int tokenDecimals) {
        BigInteger bigIntAmount = amount.getValue();

        BigDecimal bigDecAmount = new BigDecimal(bigIntAmount)
                .divide(new BigDecimal(Math.pow(10, tokenDecimals)), tokenDecimals, RoundingMode.HALF_UP);

        return bigDecAmount.toString();
    }

    public String bigIntToString(BigInteger amount, int tokenDecimals) {
        BigDecimal bigDecimalAmount = new BigDecimal(amount)
                .divide(new BigDecimal(Math.pow(10, tokenDecimals)), tokenDecimals, RoundingMode.HALF_UP);

        return bigDecimalAmount.toString();
    }

    public BigDecimal bigIntToBigDec(BigInteger amount, int tokenDecimals) {
        return new BigDecimal(amount)
                .divide(new BigDecimal(Math.pow(10, tokenDecimals)), tokenDecimals, RoundingMode.HALF_UP);
    }

    public BigDecimal uint256ToBigDecimal(final Uint256 amount, int tokenDecimals) {
        BigInteger bigIntAmount = amount.getValue();

        return new BigDecimal(bigIntAmount)
                .divide(new BigDecimal(Math.pow(10, tokenDecimals)), tokenDecimals, RoundingMode.HALF_UP);
    }
}
