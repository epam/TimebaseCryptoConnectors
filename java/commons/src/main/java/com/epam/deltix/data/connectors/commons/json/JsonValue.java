package com.epam.deltix.data.connectors.commons.json;

import io.github.green4j.jelly.AppendableWriter;
import io.github.green4j.jelly.JsonGenerator;
import io.github.green4j.jelly.JsonNumber;

import java.io.Writer;
import java.math.BigDecimal;

public final class JsonValue {
    public static JsonValue newObject() {
        return new JsonValue(new JsonObject());
    }

    public static JsonValue newArray() {
        return new JsonValue(new JsonArray());
    }

    public static JsonValue newString(final String value) {
        return new JsonValue(value);
    }

    public static JsonValue newInteger(final int value) {
        return new JsonValue(value);
    }

    public static JsonValue newLong(final long value) {
        return new JsonValue(value);
    }

    public static JsonValue newDouble(final double value) {
        return new JsonValue(value);
    }

    public static JsonValue newDecimal(final BigDecimal value) {
        return new JsonValue(value);
    }

    public static JsonValue newDecimal(final JsonNumber value) {
        return newDecimal(value.mantissa(), value.exp());
    }

    public static JsonValue newDecimal(final long mantissa, final int exp) {
        return exp == 0 ? new JsonValue(mantissa) :
                new JsonValue(BigDecimal.valueOf(mantissa, -exp));
    }

    public static JsonValue newBoolean(final boolean value) {
        return new JsonValue(value);
    }

    public static JsonValue newNull() {
        return new JsonValue(null);
    }

    static final BigDecimal ZERO = new BigDecimal(0);
    private final Object value;

    private JsonValue(final Object value) {
        this.value = value;
    }

    Object value() {
        return value;
    }

    public JsonObject asObject() {
        if (value instanceof JsonObject) {
            return (JsonObject) value;
        }
        return null;
    }

    public JsonObject asObjectRequired() {
        if (value instanceof JsonObject) {
            return (JsonObject) value;
        }
        throw new IllegalStateException("Missed required object");
    }

    public JsonArray asArray() {
        if (value instanceof JsonArray) {
            return (JsonArray) value;
        }
        return null;
    }

    public JsonArray asArrayRequired() {
        if (value instanceof JsonArray) {
            return (JsonArray) value;
        }
        throw new IllegalStateException("Missed required array");
    }

    public String asString() {
        if (value instanceof String) {
            return (String) value;
        }
        return null;
    }

    public String asStringRequired() {
        if (value instanceof String) {
            return (String) value;
        }
        throw new IllegalStateException("Missed required string");
    }

    public int asInteger() {
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        return 0;
    }

    public int asIntegerRequired() {
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        throw new IllegalStateException("Missed required integer");
    }

    public long asLong() {
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        return 0L;
    }

    public long asLongRequired() {
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        throw new IllegalStateException("Missed required long");
    }

    public double asDouble() {
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        return 0d;
    }

    public double asDoubleRequired() {
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        throw new IllegalStateException("Missed required double");
    }

    public BigDecimal asDecimal() {
        if (value instanceof Number) {
            final Number number = (Number) value;
            if (number instanceof BigDecimal) {
                return (BigDecimal) number;
            }
            final BigDecimal result;
            if (number instanceof Float ||
                    number instanceof Double) {
                result = new BigDecimal(number.doubleValue());
            } else {
                result = new BigDecimal(number.longValue());
            }
            return result;
        }
        return ZERO;
    }

    public BigDecimal asDecimalRequired() {
        final BigDecimal result = asDecimal();
        if (result == ZERO) {
            throw new IllegalStateException("Missed required decimal");
        }
        return result;
    }

    public boolean asBoolean() {
        if (value instanceof Boolean) {
            return ((Boolean) value).booleanValue();
        }
        return false;
    }

    public boolean asBooleanRequired() {
        if (value instanceof Boolean) {
            return ((Boolean) value).booleanValue();
        }
        throw new IllegalStateException("Missed required boolean");
    }

    public boolean isNull() {
        return value == null;
    }

    public void toJsonAndEoj(final JsonWriter jsonWriter) {
        toJson(jsonWriter);
        jsonWriter.eoj();
    }

    public void toJsonAndEoj(final Writer writer) {
        final JsonGenerator jsonGenerator = new JsonGenerator();
        jsonGenerator.setOutput(new AppendableWriter<>(writer));
        toJsonAndEoj(new JsonWriterToGenerator(jsonGenerator));
    }

    public void toJson(final JsonWriter jsonWriter) {
        if (isNull()) {
            jsonWriter.nullValue();
            return;
        }
        if (value instanceof JsonObject) {
            ((JsonObject) value).toJson(jsonWriter);
            return;
        }
        if (value instanceof JsonArray) {
            ((JsonArray) value).toJson(jsonWriter);
            return;
        }
        if (value instanceof String) {
            jsonWriter.stringValue((String) value, true);
            return;
        }
        if (value instanceof Number) {
            final BigDecimal decimal = asDecimal();
            jsonWriter.numberValue(decimal.unscaledValue().longValue(), -decimal.scale());
            return;
        }
        if (value instanceof Boolean) {
            if (((Boolean) value).booleanValue()) {
                jsonWriter.trueValue();
            } else {
                jsonWriter.falseValue();
            }
            return;
        }
        throw new IllegalStateException("Unsupported type of the value: " + value + ". " + value.getClass());
    }
}
