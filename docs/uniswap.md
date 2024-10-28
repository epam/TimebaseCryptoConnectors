# About Uniswap 

The Uniswap protocol is a peer-to-peer system designed for exchanging cryptocurrencies (ERC-20 tokens) on the Ethereum blockchain. 
The protocol is implemented as a set of persistent, non-upgradable smart contracts, designed to prioritize censorship resistance, security, self-custody, and to function without any trusted intermediaries who may selectively restrict access.

> Refer to [Uniswap documentation](https://docs.uniswap.org/protocol/introduction) for more information. 

# Glossary 

* **Token** is a representation of an asset, right, ownership, access, cryptocurrency, or anything else that is not unique in and of itself but can be transferred.
* **ERC-20 Token** is the technical standard for fungible tokens created using the Ethereum blockchain. [Learn more](https://www.investopedia.com/news/what-erc20-and-what-does-it-mean-ethereum/).
* **Pool** is a contract deployed by the V3 factory that pairs two ERC-20 tokens. Different pools may have different fees despite containing the same token pair. 
* **Smart contracts** are conditions written into the coding that execute different aspects of a transaction between parties.
* [Subgraph](https://thegraph.com/en/) is an open API for querying networks like Ethereum.

# Uniswap Market Data Connector

## Overview

We developed a Uniswap Market Data Connector to aggregate data from Uniswap protocol. The connector can collect two types of data from Uniswap: raw data and quotes. 

### Raw Data

Market data connector can be configured to query general information about Uniswap, information about pools, information about tokens etc. 

> Read more [here](#configuration-to-get-raw-data) to learn how to configure connector to get raw data. 

![](/docs/img/raw.png)

In this case, the connector sends requests to Uniswap via The Subgraph and writes to TimeBase stream the received data. Below, you can see what this type of data looks like in JSON format in TimeBase Web Admin application. 

![](/docs/img/tb-raw.png)

You can get the following information from Uniswap with this configuration of the connector: 

|Model Type|Description|
|----------|-----------|
|FactoryAction|Uniswap general information (e.g. number of pools, total volume of available liquidity, fees etc.)|
|BundleAction|Quotes for ETH in USD.|
|PoolAction|All pools with their details for the selected pair or a single token.|
|TokenAction|Properties of the specific token (e.g. id, name, ticker, decimals, fees, liquidity volume, total number of pools etc.)|
|PositionAction|List of all positions for the selected pair of tokens.|
|TickAction|A list of all ticks. Tick - boundaries between discrete areas in a price space.|
|SwapAction|A list of events emitted by the pool for any swaps between token0 and token1.|
|MintAction|A list of events emitted when liquidity is minted for a given position.|
|BurnAction|A list of events emitted when a position's liquidity is removed.|
|CollectAction|A list of events emitted when fees are collected by the owner of a position.|
|FlashAction|A list of events emitted by the pool for any flashes of token0/token1.|
|TransactionAction|A list of all interactions with the blockchain.|
|PositionSnapshotAction|General information about all the positions.|
|UniswapDayDataAction|Data accumulated and condensed into day stats across the entire Uniswap.|
|TokenDayDataAction|Data accumulated and condensed into day stats for the selected token.|

> Refer to [Uniswap glossary](https://docs.uniswap.org/concepts/glossary) for more information 

### Quotes

Market data connector can be configured to query quotes for tokens. We use this data to build a tiered order book.  

> Read more [here](#configuration-to-get-quotes) to learn how to configure connector to get quotes. 

![](/docs/img/quotes.png)

We developed a **Price Handler** service to collect quotes from Uniswap. Price Handler uses Uniswap Alpha Router to get the best prices from V2 and V3 protocols for specific volumes of the selected tokens. 

Quotes are returned in a JSON format as an array of prices and amounts for both market sides: 

```json
{
  "timestamp": 1661338904314,
  "asks": 
  [
    [
      21290.6492895,
      2
    ],
    [
      21293.4869725,
      4
    ]
  ],
  "bids": 
  [
    [
      21269.7888075,
      2
    ],
    [
      21264.23985775,
      4
    ],
  ]
}
```

Market Data Connector queries this data from Price Handler and uses them to build a tiered order book. Below, you can see what this type of data looks like in JSON format in TimeBase Web Admin application.

![](/docs/img/tb-l2.png)

Or visualized as an order book: 

![](/docs/img/tb-book.png)

## Configuration

### Configuration to Get Raw Data

Create an instance of the Uniswap Market Data Connector with this configuration to get raw data from Uniswap. Add this block to the [application.yaml](https://raw.githubusercontent.com/epam/TimebaseCryptoConnectors/main/java/runner/src/main/resources/application.yaml) configuration file:

```yaml
# example of a Uniswap market data connector configuration for application.yaml
uniswap:
   stream: uniswap
   type: uniswap
   model:
     - "CUSTOM"
   modelTypes:
     - "com.epam.deltix.data.uniswap.FactoryAction"
     - "com.epam.deltix.data.uniswap.BundleAction"
     - "com.epam.deltix.data.uniswap.PoolAction"
     - "com.epam.deltix.data.uniswap.TokenAction"
     - "com.epam.deltix.data.uniswap.PositionAction"
     - "com.epam.deltix.data.uniswap.TickAction"
     - "com.epam.deltix.data.uniswap.SwapAction"
     - "com.epam.deltix.data.uniswap.MintAction"
     - "com.epam.deltix.data.uniswap.BurnAction"
     - "com.epam.deltix.data.uniswap.CollectAction"
     - "com.epam.deltix.data.uniswap.FlashAction"
     - "com.epam.deltix.data.uniswap.TransactionAction"
     - "com.epam.deltix.data.uniswap.PositionSnapshotAction"
     - "com.epam.deltix.data.uniswap.UniswapDayDataAction"
     - "com.epam.deltix.data.uniswap.TokenDayDataAction"
     - "com.epam.deltix.data.uniswap.TokenHourDataAction"
   instruments: "USDC/WETH=USDC/WETH"
```

**Connector-specific parameters:**

* `type` - must be **uniswap**.
* `model` - must be **CUSTOM**.
* `modelTypes` - see the list of supported types in the table below.
* `instruments` - ERC20/ERC20  pair of tokens we create order book for.

### Configuration to Get Quotes

Create an instance of the Uniswap Market Data Connector with this configuration to get quotes for the selected tokens from Uniswap. Add this block to the [application.yaml](https://raw.githubusercontent.com/epam/TimebaseCryptoConnectors/main/java/runner/src/main/resources/application.yaml) configuration file:

```yaml
# example of configuration for application.yaml
uniswap-l2:
   stream: uniswap-l2
   type: uniswap
   depth: 50
   amount: 100
   model:
     - "L2"
   instruments: "USDC/WETH=USDC/WETH"
```

> Note: We build an order book based on quotes from V2 and V3 Uniswap protocols.

**Connector-specific parameters:**

* `type` - must be **uniswap**.
* `model` - must be L2.
* `depth` - number of levels in the order book.
* `amount` - max number of tokens for all order book levels.
* `instruments` – [token0/token1](https://www.investopedia.com/news/what-erc20-and-what-does-it-mean-ethereum/) trading pairs we create order book for.
  - `token0` – quote token.
  - `token1` – swapped token.

> Example of how we calculate the number of tokens on each level in the order book: The value of `amount` we split between the value of `depth`. E.g.: depth=50 and amount=100 --> each order book level will include 2 tokens: 100/50=2.

#### Getting Quotes

Uniswap Market Data Connector with a certain periodicity sends REST calls to **Price Handler** with the following parameters to get quotes for each pair of tokens from V2 and V3 pools and builds the tiered order book:

* `token0`
* `token1`
* `amount`
* `depth`

#### Price Handler Configuration

Configure **Price Handler** service to fetch quotes from Uniswap.

1.	Navigate to **infura.io** and create a new account. **Free infura.io plan allows up to 100k requests per day**.
2.	Click **CREATE NEW KEY** to create a new key.
    + Select **Web3 API (Formerly Ethereum)** as a **Network**.
    + Enter any **Key Name**.
3.	Click **Manage Key** and select **MAINNET** in the **Network Endpoints**. **This connector supports only MAINNET network**.
4.	Copy the **MAINNET** network endpoint.
5.	In the **config.js**, pass the **MAINNET** network endpoint as `blockchain_mainnet_node_url` property value.

## Launch

1.	Start Price Handler service:
    - In Docker: TBD
    - From the service location run `npm install` and then `node index.js`
2.	[Launch the data connector itself](https://github.com/epam/TimebaseCryptoConnectors#start-connectors):
    - Configure application.yaml
    - Run docker-compose.yml


