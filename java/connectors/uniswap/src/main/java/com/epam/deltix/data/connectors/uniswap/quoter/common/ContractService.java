package com.epam.deltix.data.connectors.uniswap.quoter.common;

import org.web3j.abi.FunctionEncoder;
import org.web3j.abi.FunctionReturnDecoder;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.Function;
import org.web3j.abi.datatypes.Type;
import org.web3j.abi.datatypes.generated.Uint8;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.methods.request.Transaction;
import org.web3j.protocol.core.methods.response.EthCall;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;

public class ContractService {
    final private Web3j w3client;
    final private String DECIMALS = "decimals";
    final private String fakeWallet = "0x09528d637deb5857dc059ddde6316d465a8b3b69";

    public ContractService(Web3j w3client) {
        this.w3client = w3client;
    }

    public int getTokenDecimals(final String tokenAddress) {

        // construct decimals smart contract function
        Function decimalsFunction = new Function(
                DECIMALS,
                Arrays.asList(),
                Collections.singletonList(new TypeReference<Uint8>() {
                }));

        // call decimal function
        List<Type> decimalsResult = callContract(decimalsFunction, tokenAddress);

        return Integer.parseInt(decimalsResult.get(0).getValue().toString());
    }

    public List<Type> callContract(final Function function, final String contractAddress) {
        EthCall response = new EthCall();
        String encodedFunction = FunctionEncoder.encode(function);

        //call smart contract function
        try {
            response = w3client.ethCall(
                            Transaction.createEthCallTransaction(fakeWallet, contractAddress, encodedFunction),
                            DefaultBlockParameterName.LATEST)
                    .sendAsync().get();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }

        return FunctionReturnDecoder.decode(response.getValue(), function.getOutputParameters());
    }
}
