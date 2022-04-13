# FINOS Data Connectors

With [TimeBase Community Edition](https://github.com/finos/TimeBase-CE) you get access to a number of [market data and trade connectors](#available-connectors) for the most popular vendors that allow receiving data and recording it in [TimeBase streams](https://kb.timebase.info/community/overview/streams). 

## Basic Principles

Most of the featured connectors share common design and implementation principles. 

![](/img/tb-ce-connectors.png)

On the above illustration you can see, that a connector may receive and transfer various types of data via REST or WebSocket connection with a specific data vendor.

We have developed an [API](https://github.com/epam/TimebaseCryptoConnectors/blob/main/docs/universal.md) to consume market data from different vendors of any level of granularity and effectively map it on the TimeBase [data model](#data-model). It includes classes that represent L1, L2, and even L3 market data, you can use later to build your Order Book. In TimeBase, all data is organized in [streams](https://kb.timebase.info/community/overview/streams) in a form of chronologically arranged [messages](https://kb.timebase.info/community/overview/messages). In object-oriented programing languages messages can be seen as classes, each with a specific set of fields.

The job of any connector is to receive, transform/model, and load data into a designated TimeBase stream. 

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

> Refer to the [API Reference](https://github.com/epam/TimebaseCryptoConnectors/blob/main/docs/universal.md) documentation to learn more.

## Quick Start Connector

### Docker Compose 

```yaml
version: "3"
services:
  timebase:
    image: "finos/timebase-ce-server:6.1"    
    stop_grace_period: 5m
    ulimits:
      nofile:
        soft: 65536
        hard: 65536
    environment:
      - JAVA_OPTS=
        -Xms8g
        -Xmx8g        
        -XX:+HeapDumpOnOutOfMemoryError
        -XX:HeapDumpPath=/timebase-home/timebase.hprof
        -Xlog:gc=debug:file=/timebase-home/GClog-TimeBase.log:time,uptime,level,tags:filecount=5,filesize=100m
    ports:
      - 8011:8011
    volumes:
      - "./timebase-home:/timebase-home"

  timebase-admin:
    image: "epam/timebase-ws-server:latest"
    environment:
      - JAVA_OPTS=
        -Xmx1g
        -Dserver.port=8099
        -Dtimebase.url=dxtick://timebase:8011
        -Dserver.compression.enabled=true
        -Dserver.compression.mime-types=text/html,text/css,application/javascript,application/json
    ports:
      - 8099:8099
    depends_on:
      - timebase

  crypto-connectors:
    image: "epam/timebase-crypto-connectors:0.1.4"
    environment:
      - JAVA_OPTS=
        -Xmx1g
        -Dserver.port=8055
        -Dtimebase.url=dxtick://timebase:8011
        -Dserver.compression.enabled=true
        -Dserver.compression.mime-types=text/html,text/css,application/javascript,application/json
    ports:
      - 8055:8055
    depends_on:
      - timebase  
```
### Gradle

## Developer Tutorials

In this section we will describe the architecture of our single-WebSocket data connectors on the example of a Coinbase Data Connector and give you simple guidelines on how to create custom connectors within the terms of the suggested framework.

### 1. Create Settings

Create settings class:

* Inherit from `DataConnectorSettings`.
* Annotate with `@ConnectorSettings("You Connector Name")`.
* Add specific connector settings.

Example of a [Coinbase Data Connector settings](https://github.com/epam/TimebaseCryptoConnectors/blob/01bbb8f3d9e3add9c0b710832a40afcc29e008a4/java/connectors/coinbase/src/main/java/com/epam/deltix/data/connectors/coinbase/CoinbaseConnectorSettings.java). 

#### TimeBase Parameters

|Parameter|Description|Required|
|---------|-----------|--------|
|url|TimeBase URL|yes|
|tbUser|TimeBase username|no|
|tbPassword|TimeBase user password|no|

#### Connector Parameters 

> Can be unique for each connector. 

|Parameter|Description|Required|
|---------|-----------|--------|
|type|Data connector type. You can run multiple instances of each connector with different parameters (e.g. to read different set of instruments from different URLs or to save dta in different TimeBase streams). In this case, each instance of the connector will have a different name but share the same `type` e.g. coinbase.|no|
|stream|TimeBase [stream](https://kb.timebase.info/community/overview/streams) name where all data will be stored.|yes|
|wsUrl|Vendor URLs|yes|
|depth|Number of levels in the Order Book.|no|
|instruments|A list of trading instruments that will be received from the vendor.|yes|
|model|Data model type.|yes|

> Refer to [application.yaml](https://github.com/epam/TimebaseCryptoConnectors/blob/main/java/runner/src/main/resources/application.yaml) example.

### 2. Create WS Feed 

Create class inherited from `DataConnector<T>`:

* Mark with [annotation](https://github.com/epam/TimebaseCryptoConnectors/blob/01bbb8f3d9e3add9c0b710832a40afcc29e008a4/java/connectors/coinbase/src/main/java/com/epam/deltix/data/connectors/coinbase/CoinbaseDataConnector.java#L7) `@Connector("You Connector Name")`
* Implement method that creates factory of `WsFeed`. Refer to the [source code](https://github.com/epam/TimebaseCryptoConnectors/blob/01bbb8f3d9e3add9c0b710832a40afcc29e008a4/java/commons/src/main/java/com/epam/deltix/data/connectors/commons/DataConnector.java#L88). 

```java
protected abstract RetriableFactory<MdFeed> doSubscribe(
MdModel.Options selected,
CloseableMessageOutputFactory outputFactory,
String... symbols);
```

### 3. Subscribe for Market Data and Parse It

1. Inherit [SingleWsFeed](https://github.com/epam/TimebaseCryptoConnectors/blob/01bbb8f3d9e3add9c0b710832a40afcc29e008a4/java/connectors/coinbase/src/main/java/com/epam/deltix/data/connectors/coinbase/CoinbaseFeed.java#:~:text=public%20class%20CoinbaseFeed%20extends%20SingleWsFeed).
2. Implement two methods of `SingleWsFeed`:

    * `prepareSubscription` method that **subscribes** for the market data. Refer to the [source code](https://github.com/epam/TimebaseCryptoConnectors/blob/01bbb8f3d9e3add9c0b710832a40afcc29e008a4/java/commons/src/main/java/com/epam/deltix/data/connectors/commons/SingleWsFeed.java#:~:text=protected%20abstract%20void%20prepareSubscription(JsonWriter%20jsonWriter%2C%20String...%20symbols)%3B).

    ```java
    protected abstract void prepareSubscription(JsonWriter jsonWriter, String... symbols);
    ```
    * `onJson` method **to parse** the received data from exchange in JSON format. Refer to the [source code](https://github.com/epam/TimebaseCryptoConnectors/blob/01bbb8f3d9e3add9c0b710832a40afcc29e008a4/java/commons/src/main/java/com/epam/deltix/data/connectors/commons/SingleWsFeed.java#:~:text=protected%20abstract%20void%20onJson(CharSequence%20data%2C%20boolean%20last%2C%20JsonWriter%20jsonWriter)%3B).

    ```java
    protected abstract void onJson(CharSequence data, boolean last, JsonWriter jsonWriter);
    ```

## Available Connectors

|Vendor|Types of Contracts|
|------|------------------|
|[BitMEX](https://github.com/epam/TimebaseCryptoConnectors/blob/main/java/connectors/bitmex/README.md)|Quanto Contract, Inverse Perpetual SWAP, Linear Perpetual, Quanto Perpetual, Linear Futures, Quanto Futures, Inverse Futures|
|[Coinbase](https://github.com/epam/TimebaseCryptoConnectors/blob/main/java/connectors/coinbase/README.md)|SPOT|
|[FTX](https://github.com/epam/TimebaseCryptoConnectors/blob/main/java/connectors/ftx/README.md)|SPOT, Linear Futures|
|[HUOBI](https://github.com/epam/TimebaseCryptoConnectors/blob/main/java/connectors/huobi-spot/README.md) SPOT|SPOT|
|[Kraken](https://github.com/epam/TimebaseCryptoConnectors/blob/main/java/connectors/kraken/README.md) SPOT|SPOT|