package com.epam.deltix.data.connectors.commons.json;

import com.epam.deltix.dfp.Decimal;
import io.github.green4j.jelly.JsonNumber;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class JsonArray {
    private final List<JsonValue> items = new ArrayList<>();

    JsonArray() {
    }

    public JsonObject addObject() {
        return addObjectValue().asObject();
    }

    public JsonObject getObject(final int index) {
        final JsonValue jsonValue = items.get(index);
        return jsonValue.asObject();
    }

    public JsonObject getObjectRequired(final int index) {
        final JsonValue jsonValue = items.get(index);
        return jsonValue.asObjectRequired();
    }

    public JsonArray addArray() {
        return addArrayValue().asArray();
    }

    public JsonArray getArray(final int index) {
        final JsonValue jsonValue = items.get(index);
        return jsonValue.asArray();
    }

    public JsonArray getArrayRequired(final int index) {
        final JsonValue jsonValue = items.get(index);
        return jsonValue.asArrayRequired();
    }

    public void addString(final String value) {
        items.add(JsonValue.newString(value));
    }

    public String getString(final int index) {
        final JsonValue jsonValue = items.get(index);
        return jsonValue.asString();
    }

    public String getStringRequired(final int index) {
        final JsonValue jsonValue = items.get(index);
        return jsonValue.asStringRequired();
    }

    public void addInteger(final int value) {
        items.add(JsonValue.newInteger(value));
    }

    public int getInteger(final int index) {
        final JsonValue jsonValue = items.get(index);
        return jsonValue.asInteger();
    }

    public int getIntegerRequired(final int index) {
        final JsonValue jsonValue = items.get(index);
        return jsonValue.asIntegerRequired();
    }

    public void addLong(final long value) {
        items.add(JsonValue.newLong(value));
    }

    public long getLong(final int index) {
        final JsonValue jsonValue = items.get(index);
        return jsonValue.asLong();
    }

    public long getLongRequired(final int index) {
        final JsonValue jsonValue = items.get(index);
        return jsonValue.asLongRequired();
    }

    public void putDouble(final double value) {
        items.add(JsonValue.newDouble(value));
    }

    public double getDouble(final int index) {
        final JsonValue jsonValue = items.get(index);
        return jsonValue.asDouble();
    }

    public double getDoubleRequired(final int index) {
        final JsonValue jsonValue = items.get(index);
        return jsonValue.asDoubleRequired();
    }

    public void addDecimal(final JsonNumber value) {
        addDecimal(BigDecimal.valueOf(value.mantissa(), -value.exp()));
    }

    public void addDecimal(final BigDecimal value) {
        items.add(JsonValue.newDecimal(value));
    }

    public BigDecimal getDecimal(final int index) {
        final JsonValue jsonValue = items.get(index);
        return jsonValue.asDecimal();
    }

    public BigDecimal getDecimalRequired(final int index) {
        final JsonValue jsonValue = items.get(index);
        return jsonValue.asDecimalRequired();
    }

    public @Decimal long getDecimal64(final int index) {
        final JsonValue jsonValue = items.get(index);
        return jsonValue.asDecimal64();
    }

    public @Decimal long getDecimal64Required(final int index) {
        final JsonValue jsonValue = items.get(index);
        return jsonValue.asDecimal64Required();
    }

    public void addBoolean(final boolean value) {
        items.add(JsonValue.newBoolean(value));
    }

    public boolean getBoolean(final int index) {
        final JsonValue jsonValue = items.get(index);
        return jsonValue.asBoolean();
    }

    public boolean getBooleanRequired(final int index) {
        final JsonValue jsonValue = items.get(index);
        return jsonValue.asBooleanRequired();
    }

    public void addNull() {
        items.add(JsonValue.newNull());
    }

    public int size() {
        return items.size();
    }

    public void clear() {
        items.clear();
    }

    public void toJson(final JsonWriter jsonWriter) {
        jsonWriter.startArray();
        for (final JsonValue item : items) {
            item.toJson(jsonWriter);
        }
        jsonWriter.endArray();
    }

    JsonValue addObjectValue() {
        final JsonValue result = JsonValue.newObject();
        items.add(result);
        return result;
    }

    JsonValue addArrayValue() {
        final JsonValue result = JsonValue.newArray();
        items.add(result);
        return result;
    }
}
