package com.epam.deltix.data.connectors.uniswap.quoter.aggregator;

import com.epam.deltix.data.connectors.uniswap.quoter.quoter.Quoter;
import com.epam.deltix.data.connectors.uniswap.quoter.quoter.V2Quoter;
import com.epam.deltix.data.connectors.uniswap.quoter.quoter.V3Quoter;

public class QuoterFactory {

    public static Quoter buildQuoter(Protocol protocolVersion, String nodeUrl, String tokenBaseAddress,
                                     String tokenQuoteAddress) {
        Quoter quoter = null;

        switch (protocolVersion) {
            case V2:
                quoter = new V2Quoter.Builder()
                        .setNodeUrl(nodeUrl)
                        .setTokenBaseAddress(tokenBaseAddress)
                        .setTokenQuoteAddress(tokenQuoteAddress)
                        .build();
                break;
            case V3:
                quoter = new V3Quoter.Builder()
                        .setNodeUrl(nodeUrl)
                        .setTokenBaseAddress(tokenBaseAddress)
                        .setTokenQuoteAddress(tokenQuoteAddress)
                        .build();
                break;
        }

        return quoter;
    }
}
