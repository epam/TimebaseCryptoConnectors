# TimeBase Crypto Market Data Connectors

[![Build CI](https://github.com/epam/TimebaseCryptoConnectors/actions/workflows/build.yml/badge.svg)](https://github.com/epam/TimebaseCryptoConnectors/actions/workflows/build.yml)

With [TimeBase Community Edition](https://github.com/finos/TimeBase-CE) you get access to a number of [market data connectors](#available-connectors) that allow receiving normalized market data with any level of granularity from the most popular crypto exchanges and recording it in [TimeBase](https://kb.timebase.info/) **in a matter of minutes**. 

## Quick Start - setup market data collection in 60 seconds:

1. Download sample [docker-compose.yml](https://github.com/epam/TimebaseCryptoConnectors/blob/main/docs/docker-compose.yml).
2. Launch Timebase crypto connectors using `docker-compose up` command. 
3. View live and historical data in your browser on [localhost:8099](http://localhost:8099/app/stream/coinbase) in [TimeBase](https://kb.timebase.info/community/development/tools/Web%20Admin/admin_guide#stream-actions-monitor):

![](/img/stream-monitor.png)

By default, we launch with [application.yaml](https://github.com/epam/TimebaseCryptoConnectors/blob/main/java/runner/src/main/resources/application.yaml#:~:text=connectors%3A,USDT%2CLTC%2DUSD%22) configuration to start all the available connectors with the default settings. Recorded market data is saved into `/timebase-home` directory. You can create custom configurations to run just the selected connectors with [specific settings](https://github.com/epam/TimebaseCryptoConnectors/blob/main/docs/developer.md#1-create-settings). 

> Refer to the [developers tutorials](https://github.com/epam/TimebaseCryptoConnectors/blob/main/docs/developer.md) for more information. 

## Supported Crypto Exchanges

|Vendor|Types of Contracts|
|------|------------------|
|[BitMEX](java/connectors/bitmex/README.md)|Quanto Contract, Inverse Perpetual SWAP, Linear Perpetual, Quanto Perpetual, Linear Futures, Quanto Futures, Inverse Futures|
|[Coinbase](https://github.com/epam/TimebaseCryptoConnectors/blob/main/java/connectors/coinbase/README.md)|SPOT|
|[FTX](https://github.com/epam/TimebaseCryptoConnectors/blob/main/java/connectors/ftx/README.md)|SPOT, Linear Futures|
|[HUOBI SPOT](https://github.com/epam/TimebaseCryptoConnectors/blob/main/java/connectors/huobi-spot/README.md)|SPOT|
|[HUOBI FUTURES](https://github.com/epam/TimebaseCryptoConnectors/tree/main/java/connectors/huobi-futures)|Inverse Futures|
|[Kraken&nbsp;SPOT](https://github.com/epam/TimebaseCryptoConnectors/tree/main/java/connectors/kraken-spot)|SPOT|
|[Kraken&nbsp;FUTURES](https://github.com/epam/TimebaseCryptoConnectors/tree/main/java/connectors/kraken-futures)|Inverse and Linear Perpetual SWAP, Inverse Futures with Expiration|
|[BYBIT&nbsp;FUTURES](https://github.com/epam/TimebaseCryptoConnectors/tree/main/java/connectors/bybit-futures)|Inverse and Linear Futures|
|[BYBIT&nbsp;SPOT](https://github.com/epam/TimebaseCryptoConnectors/tree/main/java/connectors/bybit-spot)|SPOT|
|[OKEX](https://github.com/epam/TimebaseCryptoConnectors/tree/main/java/connectors/okex)|SPOT, Linear and Inverse SWAP, Inverse and Linear Futures|
|[BITFINEX](https://github.com/epam/TimebaseCryptoConnectors/tree/main/java/connectors/bitfinex)|SPOT, Linear Futures|
|[BITPANDA](https://github.com/epam/TimebaseCryptoConnectors/tree/main/java/connectors/bitpanda)|SPOT|
|[BITMART](https://github.com/epam/TimebaseCryptoConnectors/tree/main/java/connectors/bitmart)|SPOT|
|[COINFLEX](https://github.com/epam/TimebaseCryptoConnectors/tree/main/java/connectors/coinflex)|SPOT, Linear Futures|
|[CRYPTOFACILITIES](https://github.com/epam/TimebaseCryptoConnectors/tree/main/java/connectors/cryptofacilities)|Linear Futures|
|[HITBTC](https://github.com/epam/TimebaseCryptoConnectors/tree/main/java/connectors/cryptofacilities)|SPOT|
|[POLONIEX](https://github.com/epam/TimebaseCryptoConnectors/tree/main/java/connectors/poloniex)|SPOT|
