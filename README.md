# FINOS Data Connectors

With [TimeBase Community Edition](https://github.com/finos/TimeBase-CE) you get access to a number of market data and trade connectors for the most popular vendors that allow receiving data and recording it in [TimeBase streams](https://kb.timebase.info/community/overview/streams). 

## Basic Principles

Most of the featured connectors share common design and implementation principles. 

![](/img/tb-ce-connectors.png)

On the above illustration you can see, that a connector may receive and transfer various types of data via REST or WebSocket connection with a specific data vendor.

We have developed an [API](https://github.com/epam/TimebaseCryptoConnectors/blob/main/docs/universal.md) to consume market data from different vendors of any level of granularity and effectively map it on the TimeBase data model. It includes classes that represent L1, L2, and even L3 market data, you can use later to build your Order Book. In TimeBase, all data is organized in [streams](https://kb.timebase.info/community/overview/streams) in a form of chronologically arranged [messages](https://kb.timebase.info/community/overview/messages). In object-oriented programing languages messages can be seen as classes, each with a specific set of fields.

The job of any connector is to receive, transform/model and load market data into a designated TimeBase stream. 

## Data Model

Received market data is organized in so-called Packages. `PackageHeader` class represents a package of any type of data. It includes fields that describe a message type and a message body:

* Message type is represented by one of the `PackageTypes`: 
    - `INCREMENTAL_UPDATE`: updates the market data snapshot received from the vendor.
    - `PERIODICAL_SNAPSHOT`: runtime market data snapshot collected by the data connector.
    - `VENDOR_SNAPSHOT`: marked data snapshots received directly from the vendor.
* Message body is represented by `Entries` objects, which can be one of the following types:
    - L1 represents both exchange-local top of the book (BBO) as well as National Best Bid Offer (NBBO).
    - L2 (market by level) market data snapshot provides an aggregated Order Book by price levels.
    - L3 (market by order) market data snapshot provides a detailed view into the full depth of the Order Book, individual orders size and position at every price level.
    - TradeEntry includes basic information about market trades, not specific to any granularity level. TradeEntry can be sent within L1, L2 or L3.

> Refer to the [API Reference](https://github.com/epam/TimebaseCryptoConnectors/blob/main/docs/universal.md) documentation to learn more.

## Quick Start Connector

## Developer Tutorials

### Parameters 

Most of the connectors share common configuration parameters declared in `DataConnectorSettings` class.

> Refer to [application.yaml](https://github.com/epam/TimebaseCryptoConnectors/blob/main/java/runner/src/main/resources/application.yaml) example.

#### TimeBase Parameters

|Parameter|Description|Required|
|---------|-----------|--------|
|url|TimeBase URL|yes|
|tbUser|TimeBase username|no|
|tbPassword|TimeBase user password|no|

#### Connector Parameters 

|Parameter|Description|Required|
|---------|-----------|--------|
|type|Data connector type. You can run multiple instances of each connector with different parameters (e.g. to read different set of instruments from different URLs or to save dta in different TimeBase streams). In this case each instance of the connector will have a different name but share the same type e.g. coinbase.|no|
|stream|TimeBase [stream](https://kb.timebase.info/community/overview/streams) name where all data will be stored.|yes|
|wsUrl|Vendor URLs|yes|
|depth|Number of levels in the Order Book.|no|
|instruments|A list of trading instruments that will be received from the vendor.|yes|
|model|Data model type.|yes|

```yaml
# example of Coinbase connector configuration parameters
connectors:
  coinbase:
    stream: coinbase
    wsUrl: connector URL
    model:
      - "L1"
    instruments: "XBT/US, ETH/USD"
```

## Available Connectors

|Vendor|Types of Contracts|
|------|------------------|
|[BitMEX](https://github.com/epam/TimebaseCryptoConnectors/blob/main/java/connectors/bitmex/README.md)|Quanto Contract, Inverse Perpetual SWAP, Linear Perpetual, Quanto Perpetual, Linear Futures, Quanto Futures, Inverse Futures|
|[Coinbase](https://github.com/epam/TimebaseCryptoConnectors/blob/main/java/connectors/coinbase/README.md)|SPOT|
|[FTX](https://github.com/epam/TimebaseCryptoConnectors/blob/main/java/connectors/ftx/README.md)|SPOT, Linear Futures|
|[HUOBI](https://github.com/epam/TimebaseCryptoConnectors/blob/main/java/connectors/huobi-spot/README.md) SPOT|SPOT|
|[Kraken](https://github.com/epam/TimebaseCryptoConnectors/blob/main/java/connectors/kraken/README.md) SPOT|SPOT|