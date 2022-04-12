# Universal Format

## Introduction

Deltix components may consume market data with different level of detalization from external sources. All market data is then stored in TimeBase - Deltix proprietary time-series database. In TimeBase, all data is organized in streams in a form of chronologically arranged messages. In object-oriented programing languages messages can be seen as classes, each with a specific set of fields. To be able to consume market data of any depth and detalization and map it on the Deltix data model, we developed a special library StandardMessageFormat that includes classes that represent L1, L2, and even L3 market data, which can be then used for Order Book construction.

## Model Types

Received market data is organized in so-called Packages. [PackageHeader](#packageheader) class represents a package of any type of data. It includes fields that describe a message type and a message body:

* **Message type** is represented by the PackageType: `Deltix.Timebase.Api.Messages.Universal.PackageType` in .NET and `deltix.timebase.api.messages.universal.PackageType` in JAVA. 
* **Package type** can be one of the values of the enumeration `PackageType`:
    - `INCREMENTAL_UPDATE`: updates the market data snapshot received from the vendor.
    - `PERIODICAL_SNAPSHOT`: market data snapshot sent by any Deltix component (Aggregator or market data connector).
    - `VENDOR_SNAPSHOT`: marked data snapshots received directly from the vendor.

This is important to differentiate between snapshots. `PERIODICAL_SNAPSHOT` listens to `VENDOR_SNAPSHOT` and `INCREMENTAL_UPDATE` and keeps the actual state of the Order Book. It does not provide a state change and contains data to initialize the state for new subscribers or to initialize backtesting starting from a particular state. `PERIODICAL_SNAPSHOT` sent by Deltix components may be skipped, whereas `VENDOR_SNAPSHOT` must be processed in one way or another. `INCREMENTAL_UPDATE` contains state changes sent by the vendor or as per any application request (for example, because of possible error in the current state).

![](/doc/UniversalFormat/img/snapshots.png)

Message body is represented by Entries objects, which can be one of the following types:
* [L1](#l1-best-bid-offer) represents both exchange-local top of the book (BBO) as well as National Best Bid Offer (NBBO).
* [L2](#l2-market-by-level): (e.g. FIX IncrementalRefresh/FullRefresh, CME.SBE, FAST, etc). L2 (market by level) market data snapshot provides an aggregated Order Book by price levels.
* [L3](#l2-market-by-order): (like MBO.FIX, NASDAQ ITCH, BATS.PITCH). L3 (market by order) market data snapshot provides a detailed view into the full depth of the Order Book, individual orders size and position at every price level. 
* TradeEntry: Basic information about market trades, not specific to any detalization level. TradeEntry can be sent within L1, L2 or L3.
* BookResetEntry: It is used by the market data vendor to drop the state of a particular Order Book.

Entries have the following hierarchy:

![](/doc/UniversalFormat/img/flowchart2.png)

You can use any of these types of Entries s an input data to build your Order Book. It can serve as BBO aggregator, work with Level2 data or Level3 data. You can mix different Entries and different exchanges in one Package. But it is not allowed to send snapshots for multiple exchanges within the same Package.

## PackageHeader

Classes `Deltix.Timebase.Api.Messages.Universal.PackageHeader` in .NET and `deltix.timebase.api.messages.universal.PackageHeader` in JAVA represent market data package.
`PackageHeader` represents package of any type of data. `PackageHeader` instance should have more than 0 entries, otherwise it is considered invalid. If user needs to send an empty package, `BookResetEntry` can be used instead.
`PackageHeader` may contain snapshots or incremental updates.
It is possible to mix different entries and different exchanges in one package. But it is not allowed to send snapshots for multiple exchanges within the same package. We are not supporting snapshot and increment messages mixed in one package. For example, there should not be updates or trades in the same message that contains increments. However, trades can be easily combined with increments.
If `BookResetEntry` was received, then we are waiting for snapshots and not increments.
Snapshot includes the entire state of the book for a particular exchange.

|Field|.NET|JAVA|Description|Validation|
|--|--|--|--|--|
|PackageType|`Deltix.Timebase.Api.Messages.Universal.PackageHeader.PackageType`|`deltix.timebase.api.messages.universal.PackageHeader.getPackageType()`|Package type can be one of the values of the enumeration PackageType:</br>`INCREMENTAL_UPDATE` - updates the market data snapshot received from the vendor.</br>`PERIODICAL_SNAPSHOT` - market data snapshot sent by any Deltix component (Aggregator or market data connector).</br>`VENDOR_SNAPSHOT` - marked data snapshots received directly from the vendor.|Cannot be null. PacckageType should coincide with the message entries. Please, see each model type description for detailed information.|
|Entries|`Deltix.Timebase.Api.Messages.Universal.PackageHeader.Entries`|`deltix.timebase.api.messages.universal.PackageHeader.getEntries()`|message body|Should have more than 0 entries, otherwise it is considered invalid.|

## L1 - Best Bid Offer

L1 level of market data detalization may represent both exchange-local top of the book (BBO) as well as National Best Bid Offer (NBBO). This is always a one side quote. The unique key for such data entry is a combination of symbol, exchange and side fields. The following entries can be sent within this level of detalization:

* `L1Entry`
* `TradeEntry`

The following scheme describes the process of validation of an incoming message. If the validation rules are not met, the message is rejected and no updates are made by this message. The green color of condition indicates that this is a warning case, and not a reason to consider the message invalid.

![](/doc/UniversalFormat/img/ValidationSchemeL1.png)

### Package Validation

Package validation block checks the package consistency. It is possible to mix trades from different exchanges in one package. But it is not allowed to send snapshots for multiple exchanges within one package. It is considered that snapshot of current state of the book for each exchange is sent entirely within one package. There cannot be, for example, snapshot of each side of the book sent separately in multiple packages.
Trade entry in this format can be sent as incremental update. They do not affect state of the book. 

### Fields Validation

Validator (if it is used) requires that entry size should be a number (not NaN). In some cases (for example for indicative quotes) size may be null.
Price should be a number, not null. Price should be > 0 by default. Price less or equal to 0 can be enabled in validator (for example for spreads or synthetic instruments trading). The required set of fields are: price, side, and size. It is also a warning case if bid is not less than ask within one exchange.

## L2 - Market by Level

L2 level of market data detalization describes a set of active limit orders for a certain instrument maintained by exchange. It includes prices and sizes of bids and offers, number of orders on every price level. 
The below figure illustrates a simplified example of an Order Book. The table consists of two parts: the left side shows orders for buy, the right one shows orders for sale. Each price level indicates its combined volume. The top line of the table yields the best bid and the best ask prices.

![](/doc/UniversalFormat/img/OrderBook.png)

The key for such data is symbol, exchange, side and **level index**. This means that there is a unique price entry for such combination of fields.
L2-updates insert, delete or update particular lines in the Order Book either on ask or bid side. It also can encode L2-snapshot entry. **Snapshot** is a message which reports the full Order Book state (snapshot) at once. Note L2 is price level-oriented depth-of-the-book format and should be used whenever price or integer index is used to locate Order Book changes. If incremental changes key is a **quoteId**, L3Entry should be used instead.
The following entries are supported in L2:

* `L2EntryNew`
* `L2EntryUpdate`
* `TradeEntry`

We use the following numbering of the price levels - best bid and best ask attributed to 0 level, the next prices are at the level 1 and so on. When a new limit order comes in, it will be placed in the corresponding Order Book level according to its price.
It is possible to pass data to the Order Book with fixed or non-fixed Market Depth. **Market Depth** is the number of price Levels in the Order Book. It is specified by Level2 data provider or the exchange itself. Hence, it could be different for different data providers. By design, it is considered that the depth is provided by means of other data messages, or even using some other approach.
If the Order Book has a fixed depth, Market Depth is to be provided, or, alternatively, the data provider should send deletes to higher levels if this is the convention. Setting up the correct value of a Market Depth is important when creating an Order Book. Failure to do so results in an incorrect simulation:

* If user specifies the Market Depth that is greater than the real one, Order Book can hold outdated orders.
* If user specifies the Market Depth that is smaller than the real one, Order Book can lose new orders.

Levels on top of the Market Depth will be dropped. For example if Market Depth is 10 and one more level is added before 10th, 10th level will be dropped. If DELETE happens after that, the Order Book will have only 9 price levels.
The following scheme describes the process of validation of an incoming message. If the validation rules are not met, the message is rejected and no updates are made to Order Book by this message. The green color of condition indicates that this is a warning case, and not a reason to consider the message invalid.

![](/doc/UniversalFormat/img/ValidationSchemeL2.png)

### Package Validation

Package validation block checks the package consistency. It is possible to mix different entries within one package, or entries from different exchanges in one package. But it is not allowed to send snapshots for multiple exchanges within one package. There also should not be snapshot entries combined with update entries in one snapshot message. But trades and updates can be sent in one package. It is considered that a snapshot of a current state of the Order Book for each exchange is sent entirely within one package. There cannot be, for example, snapshots of each side of the Order Book sent separately in multiple packages.
Trade entry can be sent in this format within a package of incremental updates. They do not affect states of the Order Book.

### Fields Validation

The following rules are applied to the values of the fields:

* Side and size should have valid values. Size should be > 0, a number and not null.
* Price should be a number, not null. Price should be > 0 by default. Price less or equal to 0 can be enabled in validator (for example for spreads or synthetic instruments trading). 
* Level should be less than the Market Depth. 
* For an update, Action can be one of the following:
    - `INSERT`: inserting a new quote on the specified level (specified in the Level property). If such level already exists, every index of the level that is greater or equal than this will be incremented (For instance, if level 5 is added then levels' index from 5 and greater will be incremented by 1). `L2Entry` update with insert type is considered invalid. `L2EntryNew` should be sent in this case. 
    - `UPDATE`: updating values on the specified level. Participant id, quote id and number of orders can be updated.
    - `DELETE`: deletes the level. All levels' index after this level is decremented. Side, price, level, action fields should not be null. DELETE is used to remove quotes because trades do not do that in L2 format. They cannot affect the state of the book (like they do in L3 format).

### Conformity with the State of the Book

* **Sorting**: The design allows sending data to an aggregated or non-aggregated Order Book, in other words, we can aggregate orders per price level. In case of an aggregated Order Book, prices on levels should be strictly ascending for ask side and strictly descending for bid side. In case of a  non-aggregated Order Book, prices on levels should be monotonically ascending for ask side and monotonically descending for bid side (there can be more than one level in a row with the same price, denoting an individual order):
    - An aggregated Order Book is correct, when:</br>
        + Bid0 < Ask0
        + BidN > BidN+1
        + AskN < AskN+1
    - A non-aggregated Order Book is correct, when:</br>
        + Bid0 < Ask0
        + BidN >= BidN+1
        + AskN <= AskN+1</br>
    The provider should send packages during processing each part of which levels should remain sorted. Therefore after processing each entry in package Order Book should be ordered. Otherwise processing algorithms should have been significantly more complicated. It is critical that data is sorted on each side for each exchange, but if best bid is not less than best ask it is considered as a warning case. Such data are still valid.
* For non-aggregated Order Book we allow to use `QuoteId`, but as it was said earlier, `QuoteId` is not the key of the data. If `QuoteId` field value coincides for multiple quotes then this is a warning case. In other words, it is at data provider discretion to provide quotes with unique id.
* It is important that values in fields of the update coincide with the actual values in Order Book. For example, price in update is the same as price in Order Book for this level. Update cannot change price.

## L3 - Market by Order

L3 format is designed to represent an Order Book as a set of individual quotes for each price level.
The key of the data entry is symbol, exchange, side and **quote id**.
The orders are sorted by price, and quotes within one price level form a queue (if a particular exchange operates with orders priority).
L3-updates: new, cancel, modify and replace of one quote in Order Book either on ask or bid side. It can also encode L3-snapshot entry. Note, L3 is a quote-oriented depth-of-the-book format and should be used whenever **quoteId** is used to locate the Order Book changes.

![](/doc/UniversalFormat/img/L3OrderBook.png)

The following entries can be sent within this level of detalization:

* `L3EntryNew`
* `L3EntryUpdate`
* `TradeEntry`

If `TradeEntry` is sent in L3 format (with `buyerOrderId` or `sellerOrderId` depending on `AgressiveSide`), sending such `TradeEntry` decreases order size with id equal to buyer or seller id. The following scheme describes the process of validation of an incoming message. If the validation rules are not met, the message is rejected and no updates are made to Order Book by this message.

![](/doc/UniversalFormat/img/ValidationSchemeL3.png)

### Package Validation

If `L3EntryNew` is used to provide a snapshot of Order Book state, it is considered that snapshot for a particular exchange is sent completely within one package. Snapshot entries and updates entries should not be grouped in one snapshot package. Trade entries can be grouped with incremental updates. Snapshots for multiple exchanges should be sent in different packages.

### Fields Validation

* Side, QuoteId should not be null.
* Size should be > 0, a number and not null.
* Price should be a number, not null. Price should be > 0 by default. Price less or equal to 0 can be enabled in validator (for example for spreads or synthetic instruments trading). 
* `L3EntryNew` should specify a type of the insert, which allows managing the order queue. The queue is formed within a price level. The type of insert can be:
    - `ADD_BACK` - add quote in the end of the queue;
    - `ADD_FRONT` - add quote in the beginning of the queue;
    - `ADD_BEFORE` - add quote before the quote with the id specified in the specially provided field `InsertBeforeQuoteId`. 
* Incremental updates can be:
    - `MODIFY` - change order with saving its priority in the queue;
    - `REPLACE` - change order but the priority is lost;
    - `CANCEL` - remove order from the queue.
* In case of a snapshot, type of the insert should be filled; in case of an update, type of the update should be filled.

### Conformity with the State of the Book

* For `REPLACE` update, there are no restrictions as for how price or size of orders can be changed. For `MODIFY` update, the price should coincide with the order price in the Order Book.
* `QuoteId` must be unique for each quote.
* `MODIFY` for order that has side that does not coincide with side of this order stored in the Order Book is invalid. For `REPLACE`, side is allowed to be changed.
* In case of inserting before some quote in a queue, the quote, before which we insert this quote, should be on the same price level.

### Relation to Trading Actions

The L3 format types reflect the actual trading actions. For example, the following messages are expected in L3 after these trading actions:

* `CancelReplace` -> `REPLACE`
* `Replace` -> `MODIFY`
* `Cancel` -> `CANCEL`
* `NewOrder` -> `ADD_BACK`
