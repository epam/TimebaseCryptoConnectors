
# Tutorials for Developers

## Basic Principles

Most of the featured connectors share common design and implementation principles. 

![](/docs/img/tb-ce-connectors2.png)

On the above illustration you see, that connectors use a special framework to subscribe for market data via a WebSocket connection with a specific data vendor and to write it to [TimeBase](https://github.com/finos/TimeBase-CE). In TimeBase, all data is organized in [streams](https://kb.timebase.info/community/overview/streams) in a form of chronologically arranged [messages](https://kb.timebase.info/community/overview/messages). In object-oriented programing languages messages can be seen as classes, each with a specific set of fields.

We have developed a Universal Format [API](https://github.com/epam/TimebaseCryptoConnectors/blob/main/docs/universal.md) to consume market data from different vendors of any level of granularity and effectively map it on the TimeBase [data model](#data-model). It includes classes that represent L1, L2, and even L3 market data, you can use later to build your Order Book. 

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
    - `TradeEntry` includes basic information about market trades, not specific to any granularity level. TradeEntry can be sent within L1, L2 or L3.
    - `BookResetEntry`: it is used by the market data vendor to drop the state of a particular Order Book.

> Refer to the [API Reference](https://github.com/epam/TimebaseCryptoConnectors/blob/main/docs/universal.md) documentation to learn more about the data model.


## Developing Data Connector

In this section we will describe the architecture of our single-WebSocket data connectors on the example of a Coinbase Data Connector and give you simple guidelines on how to create custom connectors within the terms of the suggested framework.

### 1. Create Settings

Create a data connector settings class:

* Inherit your data connector settings class from `DataConnectorSettings` class.
* Annotate with `@ConnectorSettings("You Connector Name")`.
* Add specific connector settings.

#### TimeBase Default Settings

|Parameter|Description|Required|
|---------|-----------|--------|
|url|TimeBase URL|yes|
|tbUser|TimeBase username|no|
|tbPassword|TimeBase user password|no|

> Refer to the [application.yaml](https://github.com/epam/TimebaseCryptoConnectors/blob/main/java/runner/src/main/resources/application.yaml#:~:text=timebase%3A,dxtick%3A//localhost%3A8011).

#### Common Settings for Connectors

|Parameter|Description|Required|
|---------|-----------|--------|
|type|Data connector type. You can run multiple instances of each connector with different parameters (e.g. to read different set of instruments from different URLs or to save dta in different TimeBase streams). In this case, each instance of the connector will have a different `name` but share the same `type` e.g. coinbase. `Type` must match the `"You Connector Name"`. Can avoid `type` if the connector instance `name` is the same as `type` name.|no|
|stream|TimeBase [stream](https://kb.timebase.info/community/overview/streams) name where all data will be stored.|yes|
|instruments|A list of trading instruments that will be received from the vendor.|yes|
|model|Data model type.|yes|

> Refer to [application.yaml](https://github.com/epam/TimebaseCryptoConnectors/blob/main/java/runner/src/main/resources/application.yaml#:~:text=connectors%3A,USDT%2CLTC%2DUSD%22).

#### Connector-Specific Settings 

> Can be unique for each connector. Here we describe a Coinbase connector settings for example purposes. 

|Parameter|Description|Required|
|---------|-----------|--------|
|wsUrl|Vendor URL|yes|
|depth|Number of levels in the Order Book.|no|

> Example of a [Coinbase Data Connector settings](https://github.com/epam/TimebaseCryptoConnectors/blob/01bbb8f3d9e3add9c0b710832a40afcc29e008a4/java/connectors/coinbase/src/main/java/com/epam/deltix/data/connectors/coinbase/CoinbaseConnectorSettings.java). 

### 2. Create DataConnector Class

Create a class inherited from `DataConnector<T>` where `T` is your settings class created on [step 1](https://github.com/epam/TimebaseCryptoConnectors#1-create-settings).

* Mark with [annotation](https://github.com/epam/TimebaseCryptoConnectors/blob/01bbb8f3d9e3add9c0b710832a40afcc29e008a4/java/connectors/coinbase/src/main/java/com/epam/deltix/data/connectors/coinbase/CoinbaseDataConnector.java#L7) `@Connector("You Connector Name")`
* Implement method that creates factory of `WsFeed`. Refer to the [source code](https://github.com/epam/TimebaseCryptoConnectors/blob/01bbb8f3d9e3add9c0b710832a40afcc29e008a4/java/connectors/coinbase/src/main/java/com/epam/deltix/data/connectors/coinbase/CoinbaseDataConnector.java#L21). 

```java
protected abstract RetriableFactory<MdFeed> doSubscribe(
MdModel.Options selected,
CloseableMessageOutputFactory outputFactory,
String... symbols);
```

### 3. Subscribe for Market Data and Parse It

1. Create [WsFeed](https://github.com/epam/TimebaseCryptoConnectors/blob/01bbb8f3d9e3add9c0b710832a40afcc29e008a4/java/connectors/coinbase/src/main/java/com/epam/deltix/data/connectors/coinbase/CoinbaseFeed.java#:~:text=public%20class%20CoinbaseFeed%20extends%20SingleWsFeed) class for your connector that inherits from [SingleWsFeed](https://github.com/epam/TimebaseCryptoConnectors/blob/01bbb8f3d9e3add9c0b710832a40afcc29e008a4/java/commons/src/main/java/com/epam/deltix/data/connectors/commons/SingleWsFeed.java#L21) class.
2. Implement two methods of `SingleWsFeed` class:
    * Implement a [prepareSubscription](https://github.com/epam/TimebaseCryptoConnectors/blob/01bbb8f3d9e3add9c0b710832a40afcc29e008a4/java/connectors/coinbase/src/main/java/com/epam/deltix/data/connectors/coinbase/CoinbaseFeed.java#L51) method of a [SingleWsFeed](https://github.com/epam/TimebaseCryptoConnectors/blob/01bbb8f3d9e3add9c0b710832a40afcc29e008a4/java/commons/src/main/java/com/epam/deltix/data/connectors/commons/SingleWsFeed.java#L214) class that **subscribes** for the market data.
    * Implement a [onJson](https://github.com/epam/TimebaseCryptoConnectors/blob/01bbb8f3d9e3add9c0b710832a40afcc29e008a4/java/connectors/coinbase/src/main/java/com/epam/deltix/data/connectors/coinbase/CoinbaseFeed.java#L77) method of a [SingleWsFeed](https://github.com/epam/TimebaseCryptoConnectors/blob/01bbb8f3d9e3add9c0b710832a40afcc29e008a4/java/commons/src/main/java/com/epam/deltix/data/connectors/commons/SingleWsFeed.java#L221) class **to parse** the received data from exchange in JSON format and write it to TimeBase stream.

![](/docs/img/tb-ce-connectors3.png)