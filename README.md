# TimeBase Data Connectors

With [TimeBase Community Edition](https://github.com/finos/TimeBase-CE) you get access to a number of [market data connectors](#available-connectors) that allow receiving normalized market data with any level of granularity from the most popular crypto exchanges and recording it in [TimeBase](https://kb.timebase.info/) **in a matter of minutes**. 

## Quick Start

1. Download sample [docker-compose.yml](https://github.com/epam/TimebaseCryptoConnectors/blob/main/docs/docker-compose.yml).
2. Run `docker-compose up`.
3. View the data running on [localhost:8099](http://localhost:8099) in [TimeBase Web Admin](https://kb.timebase.info/community/development/tools/Web%20Admin/admin_guide#stream-actions-monitor) application.

![](/img/stream-monitor.png)

> Refer to the [developers tutorials](https://github.com/epam/TimebaseCryptoConnectors/blob/main/docs/developer.md) for more information. 

## Available Connectors

|Vendor|Types of Contracts|
|------|------------------|
|[BitMEX](java/connectors/bitmex/README.md)|Quanto Contract, Inverse Perpetual SWAP, Linear Perpetual, Quanto Perpetual, Linear Futures, Quanto Futures, Inverse Futures|
|[Coinbase](https://github.com/epam/TimebaseCryptoConnectors/blob/main/java/connectors/coinbase/README.md)|SPOT|
|[FTX](https://github.com/epam/TimebaseCryptoConnectors/blob/main/java/connectors/ftx/README.md)|SPOT, Linear Futures|
|[HUOBI SPOT](https://github.com/epam/TimebaseCryptoConnectors/blob/main/java/connectors/huobi-spot/README.md)|SPOT|
|[HUOBI FUTURES](https://github.com/epam/TimebaseCryptoConnectors/tree/main/java/connectors/huobi-futures)|Inverse Futures|
|[Kraken SPOT](https://github.com/epam/TimebaseCryptoConnectors/tree/main/java/connectors/kraken-spot)|SPOT|
|[Kraken FUTURES](https://github.com/epam/TimebaseCryptoConnectors/tree/main/java/connectors/kraken-futures)|Inverse and Linear Perpetual SWAP, Inverse Futures with Expiration|
|[BYBIT FUTURES](https://github.com/epam/TimebaseCryptoConnectors/tree/main/java/connectors/bybit-futures)|Inverse and Linear Futures|
|[BYBIT SPOT](https://github.com/epam/TimebaseCryptoConnectors/tree/main/java/connectors/bybit-spot)|SPOT|
|[OKEX](https://github.com/epam/TimebaseCryptoConnectors/tree/main/java/connectors/okex)|SPOT, Linear and Inverse SWAP, Inverse and Linear Futures|

