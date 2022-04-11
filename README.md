# FINOS Data Connectors

With [TimeBase Community Edition](https://github.com/finos/TimeBase-CE) you get access to a number of market data and trade connectors for the most popular vendors that allow receiving data and recording it in [TimeBase streams](https://kb.timebase.info/community/overview/streams). 

## Basic Principles

Most of the featured connectors share common design and implementation principles. 

![](/img/tb-ce-connectors.png)

On the above illustration you can see, that a connector may receive and transfer various types of data via REST or WebSocket connection with a specific data vendor.

We have developed a [Universal Format library]() to consume market data from different vendors of any level of detalization and effectively map it on the TimeBase data model. This library includes classes that represent L1, L2, and even L3 market data, you can use later to build your Order Book. In TimeBase, all data is organized in [streams](https://kb.timebase.info/community/overview/streams) in a form of chronologically arranged [messages](https://kb.timebase.info/community/overview/messages). In object-oriented programing languages messages can be seen as classes, each with a specific set of fields.

The job of any connector is to receive, transform/model and load market data into a designated TimeBase stream. 

## Data Formats

Received market data is organized in so-called Packages. `PackageHeader` class represents a package of any type of data. It includes fields that describe a message type and a message body:

* Message type is represented by one of the `PackageTypes`: 
    - `INCREMENTAL_UPDATE`: updates the market data snapshot received from the vendor.
    - `PERIODICAL_SNAPSHOT`: market data snapshot sent by any Deltix component (Aggregator or market data connector).
    - `VENDOR_SNAPSHOT`: marked data snapshots received directly from the vendor.
* Message body is represented by `Entries` objects, which can be one of the following types:
    - L1 represents both exchange-local top of the book (BBO) as well as National Best Bid Offer (NBBO).
    - L2 (market by level) market data snapshot provides an aggregated Order Book by price levels.
    - L3 (market by order) market data snapshot provides a detailed view into the full depth of the Order Book, individual orders size and position at every price level.
    - TradeEntry includes basic information about market trades, not specific to any detalization level. TradeEntry can be sent within L1, L2 or L3.

> Refer to the [Universal Format]() documentation to learn more.

## Connector Parameters 

## Quick Start Connector

## Docker Compose

## Developer Tutorials 