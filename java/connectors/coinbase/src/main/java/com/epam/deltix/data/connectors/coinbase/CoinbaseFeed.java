package com.epam.deltix.data.connectors.coinbase;

import com.epam.deltix.data.connectors.commons.*;
import com.epam.deltix.data.connectors.commons.json.JsonArray;
import com.epam.deltix.data.connectors.commons.json.JsonObject;
import com.epam.deltix.data.connectors.commons.json.JsonValue;
import com.epam.deltix.data.connectors.commons.json.JsonValueParser;
import com.epam.deltix.data.connectors.commons.json.JsonWriter;
import com.epam.deltix.dfp.Decimal64Utils;
import com.epam.deltix.timebase.messages.TimeStampedMessage;
import com.epam.deltix.timebase.messages.TypeConstants;
import com.epam.deltix.timebase.messages.universal.AggressorSide;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.UnsupportedEncodingException;
import java.time.Instant;
import java.util.Arrays;
import java.util.Base64;

public class CoinbaseFeed extends MdSingleWsFeed {

    private static final String SIGN_MESSAGE = "GET/users/self/verify";
    private static final String MAC_ALGORITHM = "HmacSHA256";

    // all fields are used by one single thread of WsFeed's ExecutorService
    private final JsonValueParser jsonParser = new JsonValueParser();
    private final Iso8601DateTimeParser dtParser = new Iso8601DateTimeParser();

    private final CoinbaseConnectorSettings settings;

    private final Mac mac;

    public CoinbaseFeed(
            final CoinbaseConnectorSettings settings,
            final MdModel.Options selected,
            final CloseableMessageOutput output,
            final ErrorListener errorListener,
            final Logger logger,
            final String... symbols) {

        super("COINBASE", settings.getWsUrl(), settings.getDepth(), 5000, selected, output, errorListener, logger, symbols);

        this.settings = settings;

        if (settings.getApiKey() == null || settings.getApiKey().isEmpty()) {
            logger.warning("`apiKey` property is not specified. Authentication may be required.");
        }
        if (settings.getPassphrase() == null || settings.getPassphrase().isEmpty()) {
            logger.warning("`passphrase` property is not specified. Authentication may be required.");
        }
        if (settings.getApiSecret() == null || settings.getApiSecret().isEmpty()) {
            logger.warning("`apiSecret` property is not specified. Authentication may be required.");
            mac = null;
        } else {
            try {
                mac = Mac.getInstance(MAC_ALGORITHM);
                byte[] decodedBytes = Base64.getDecoder().decode(settings.getApiSecret());
                mac.init(new SecretKeySpec(decodedBytes, MAC_ALGORITHM));
            } catch (Throwable e) {
                throw new RuntimeException(e);
            }
        }
    }

    @Override
    protected void subscribe(
            final JsonWriter jsonWriter,
            final String... symbols) {

        final JsonValue subscriptionJson = JsonValue.newObject();

        final JsonObject body = subscriptionJson.asObject();
        body.putString("type", "subscribe");

        final JsonArray productIds = body.putArray("product_ids");
        Arrays.stream(symbols).forEach(productIds::addString);

        final JsonArray channels = body.putArray("channels");
        if (selected().trades()) {
            channels.addString("ticker");
        }
        if (selected().level1() || selected().level2()) {
            channels.addString("level2");
        }

        long timestamp = Instant.now().toEpochMilli() / 1000;

        if (mac != null && settings.getApiKey() != null && settings.getPassphrase() != null) {
            body.putString("signature", calculateSign(timestamp));
            body.putString("key", settings.getApiKey());
            body.putString("passphrase", settings.getPassphrase());
            body.putString("timestamp", String.valueOf(timestamp));
        }

        subscriptionJson.toJsonAndEoj(jsonWriter);
    }

    private String calculateSign(long timestamp) {
        try {
            String message = timestamp + SIGN_MESSAGE;
            byte[] hmacSha256 = mac.doFinal(message.getBytes("UTF-8"));
            return Base64.getEncoder().encodeToString(hmacSha256);
        } catch (UnsupportedEncodingException e) {
            logger().warning("Failed to calculate signature.", e);
        }

        return "";
    }

    @Override
    protected void onJson(
            final CharSequence data,
            final boolean last,
            final JsonWriter jsonWriter) {

        jsonParser.parse(data);

        if (!last) {
            return;
        }

        final JsonValue jsonValue = jsonParser.eoj();

        final JsonObject object = jsonValue.asObjectRequired();
        final String type = object.getString("type");

        if (type == null) {
            return;
        }

        switch (type) {
            case "ticker": {
                final String productId = object.getStringRequired("product_id");
                String tradeDirection = object.getString("side");
                AggressorSide side = "buy".equalsIgnoreCase(tradeDirection) ? AggressorSide.BUY : AggressorSide.SELL;

                processor().onTrade(
                        productId,
                        TimeStampedMessage.TIMESTAMP_UNKNOWN,
                        object.getDecimal64Required("price"),
                        object.getDecimal64Required("last_size"),
                        side);
                break;
            }

            case "snapshot": {
                final String instrument = object.getStringRequired("product_id");
                final QuoteSequenceProcessor quotesListener = processor().onBookSnapshot(instrument, TimeStampedMessage.TIMESTAMP_UNKNOWN);
                processSnapshotSide(quotesListener, object.getArray("bids"), false);
                processSnapshotSide(quotesListener, object.getArray("asks"), true);
                quotesListener.onFinish();
                break;
            }

            case "l2update": {
                final String instrument = object.getStringRequired("product_id");
                dtParser.set(object.getStringRequired("time"));
                final QuoteSequenceProcessor quotesListener = processor().onBookUpdate(instrument, dtParser.millis());
                processChanges(quotesListener, object.getArrayRequired("changes"));
                quotesListener.onFinish();
                break;
            }

            default:
                break;
        }
    }

    private void processSnapshotSide(
            final QuoteSequenceProcessor quotesListener,
            final JsonArray quotePairs,
            final boolean ask) {

        if (quotePairs == null) {
            return;
        }

        for (int i = 0; i < quotePairs.size(); i++) {
            final JsonArray pair = quotePairs.getArrayRequired(i);
            if (pair.size() != 2) {
                throw new IllegalArgumentException("Unexpected size of "
                        + (ask ? "an ask" : "a bid")
                        + " quote: "
                        + pair.size());
            }
            quotesListener.onQuote(
                pair.getDecimal64Required(0),
                pair.getDecimal64Required(1),
                ask
            );
        }
    }

    private void processChanges(
            final QuoteSequenceProcessor quotesListener,
            final JsonArray changes) {

        if (changes == null) {
            return;
        }

        for (int i = 0; i < changes.size(); i++) {
            final JsonArray change = changes.getArrayRequired(i);
            if (change.size() != 3) {
                throw new IllegalArgumentException("Unexpected size of a change :" + change.size());
            }

            long size = change.getDecimal64Required(2);
            if (Decimal64Utils.isZero(size)) {
                size = TypeConstants.DECIMAL_NULL; // means delete the price
            }

            quotesListener.onQuote(
                change.getDecimal64Required(1),
                size,
                "sell".equals(change.getStringRequired(0))
            );
        }
    }
}
