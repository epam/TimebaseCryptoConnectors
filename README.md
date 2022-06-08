# TimeBase Crypto Market Data Connectors

[![Build CI](https://github.com/epam/TimebaseCryptoConnectors/actions/workflows/build.yml/badge.svg)](https://github.com/epam/TimebaseCryptoConnectors/actions/workflows/build.yml)

With [TimeBase Community Edition](https://github.com/finos/TimeBase-CE) you get access to free [market data connectors](#supported-crypto-exchanges) you can use to receive normalized market data with any level of granularity (top of the book, L2) from the most popular crypto exchanges and recording it in [TimeBase](https://kb.timebase.info/) **in a matter of minutes**. 

## Quick Start - setup market data collection in 60 seconds:

### Prerequisites

**Windows** 

* [Install Docker](https://docs.docker.com/desktop/windows/install/) on your Windows machine. 
* [Install WSL](https://docs.microsoft.com/en-us/windows/wsl/install). 

**Linux**

* [Install Docker Compose](https://docs.docker.com/compose/install/).  
* [Install Docker Engine](https://docs.docker.com/engine/install/).

### Start Connectors 

1. Download sample [docker-compose.yml](https://raw.githubusercontent.com/epam/TimebaseCryptoConnectors/main/docs/docker-compose.yml)
2. Run `docker-compose up` command from the directory with your docker-compose.yml file to launch TimeBase crypto connectors. 
3. View live and historical market data stored in [TimeBase](https://kb.timebase.info/community/development/tools/Web%20Admin/admin_guide#stream-actions-monitor) in your browser at [localhost:8099](http://localhost:8099) (default username **admin** and password **admin**):

[![Watch the video]()]([https://youtu.be/nTQUwghvy5Q](https://videoportal.epam.com/video/VYGn2Lav))

![](/img/stream-monitor.png)

### Configuration 

By default, we launch with the [default](https://raw.githubusercontent.com/epam/TimebaseCryptoConnectors/main/java/runner/src/main/resources/application.yaml) configuration to start all the available data connectors. Recorded market data is saved into `/timebase-home` directory. You can create custom configurations to run just the selected connectors with specific settings.

> Refer to the [configuration](https://github.com/epam/TimebaseCryptoConnectors/blob/main/docs/configuration.md) to learn how to configure. 

> Refer to the [developers tutorials](https://github.com/epam/TimebaseCryptoConnectors/blob/main/docs/developer.md) for more in depth information about data model and tutorials on how to develop custom data connectors. 

## Supported Crypto Exchanges

|Exchange|Supported Contracts|
|------|------------------|
|[BYBIT&nbsp;FUTURES](https://github.com/epam/TimebaseCryptoConnectors/tree/main/java/connectors/bybit-futures)|Inverse and Linear Futures|
|[BYBIT&nbsp;SPOT](https://github.com/epam/TimebaseCryptoConnectors/tree/main/java/connectors/bybit-spot)|SPOT|
|[BITFINEX](https://github.com/epam/TimebaseCryptoConnectors/tree/main/java/connectors/bitfinex)|SPOT, Linear Futures|
|[BITMART](https://github.com/epam/TimebaseCryptoConnectors/tree/main/java/connectors/bitmart)|SPOT|
|[BITPANDA](https://github.com/epam/TimebaseCryptoConnectors/tree/main/java/connectors/bitpanda)|SPOT|
|[BitMEX](java/connectors/bitmex/README.md)|Quanto Contract, Inverse Perpetual SWAP, Linear Perpetual, Quanto Perpetual, Linear Futures, Quanto Futures, Inverse Futures|
|[Coinbase](https://github.com/epam/TimebaseCryptoConnectors/blob/main/java/connectors/coinbase/README.md)|SPOT|
|[COINFLEX](https://github.com/epam/TimebaseCryptoConnectors/tree/main/java/connectors/coinflex)|SPOT, Linear Futures|
|[CRYPTOFACILITIES](https://github.com/epam/TimebaseCryptoConnectors/tree/main/java/connectors/cryptofacilities)|Linear Futures|
|[FTX](https://github.com/epam/TimebaseCryptoConnectors/blob/main/java/connectors/ftx/README.md)|SPOT, Linear Futures|
|[HITBTC](https://github.com/epam/TimebaseCryptoConnectors/tree/main/java/connectors/cryptofacilities)|SPOT|
|[HUOBI FUTURES](https://github.com/epam/TimebaseCryptoConnectors/tree/main/java/connectors/huobi-futures)|Inverse Futures|
|[HUOBI SPOT](https://github.com/epam/TimebaseCryptoConnectors/blob/main/java/connectors/huobi-spot/README.md)|SPOT|
|[Kraken&nbsp;FUTURES](https://github.com/epam/TimebaseCryptoConnectors/tree/main/java/connectors/kraken-futures)|Inverse and Linear Perpetual SWAP, Inverse Futures with Expiration|
|[Kraken&nbsp;SPOT](https://github.com/epam/TimebaseCryptoConnectors/tree/main/java/connectors/kraken-spot)|SPOT|
|[OKEX](https://github.com/epam/TimebaseCryptoConnectors/tree/main/java/connectors/okex)|SPOT, Linear and Inverse SWAP, Inverse and Linear Futures|
|[POLONIEX](https://github.com/epam/TimebaseCryptoConnectors/tree/main/java/connectors/poloniex)|SPOT|
