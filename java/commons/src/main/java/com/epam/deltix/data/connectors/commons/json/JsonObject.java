package com.epam.deltix.data.connectors.commons.json;

import com.epam.deltix.dfp.Decimal;
import com.epam.deltix.dfp.Decimal64Utils;
import io.github.green4j.jelly.AppendableWriter;
import io.github.green4j.jelly.JsonGenerator;
import io.github.green4j.jelly.JsonNumber;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Similar to JSON.simple library. When we don't worry about memory and CPU and
 * we don't wont to have a lot of POJOs just for JSON serialization/deserialization.
 */
public class JsonObject {
    private static final JsonValue NULL_VALUE = JsonValue.newNull();

    private final List<Member> members = new ArrayList<>();
    private final Map<String, JsonValue> membersByName = new HashMap<>();

    JsonObject() {
    }

    public boolean hasMember(final String name) {
        return membersByName.containsKey(name);
    }

    public JsonObject putObject(final String member) {
        return putMember(member, JsonValue.newObject()).asObject();
    }

    public JsonObject getObject(final String member) {
        final JsonValue jsonValue = membersByName.get(member);
        return jsonValue != null ? jsonValue.asObject() : null;
    }

    public JsonObject getObjectRequired(final String member) {
        final JsonValue jsonValue = membersByName.get(member);
        return jsonValue != null ?
                jsonValue.asObjectRequired() :
                NULL_VALUE.asObjectRequired();
    }

    public JsonArray putArray(final String member) {
        return putMember(member, JsonValue.newArray()).asArray();
    }

    public JsonArray getArray(final String member) {
        final JsonValue jsonValue = membersByName.get(member);
        return jsonValue != null ? jsonValue.asArray() : null;
    }

    public JsonArray getArrayRequired(final String member) {
        final JsonValue jsonValue = membersByName.get(member);
        return jsonValue != null ?
                jsonValue.asArrayRequired() :
                NULL_VALUE.asArrayRequired();
    }

    public void putString(final String member, final String value) {
        putMember(member, JsonValue.newString(value));
    }

    public String getString(final String member) {
        final JsonValue jsonValue = membersByName.get(member);
        if (jsonValue == null) {
            return null;
        }
        return jsonValue.asString();
    }

    public String getStringRequired(final String member) {
        final JsonValue jsonValue = membersByName.get(member);
        return jsonValue != null ?
                jsonValue.asStringRequired() :
                NULL_VALUE.asStringRequired();
    }

    public void putInteger(final String member, final int value) {
        putMember(member, JsonValue.newInteger(value));
    }

    public int getInteger(final String member) {
        final JsonValue jsonValue = membersByName.get(member);
        if (jsonValue == null) {
            return 0;
        }
        return jsonValue.asInteger();
    }

    public int getIntegerRequired(final String member) {
        final JsonValue jsonValue = membersByName.get(member);
        return jsonValue != null ?
                jsonValue.asIntegerRequired() :
                NULL_VALUE.asIntegerRequired();
    }

    public void putLong(final String member, final long value) {
        putMember(member, JsonValue.newLong(value));
    }

    public long getLong(final String member) {
        final JsonValue jsonValue = membersByName.get(member);
        if (jsonValue == null) {
            return 0L;
        }
        return jsonValue.asLong();
    }

    public long getLongRequired(final String member) {
        final JsonValue jsonValue = membersByName.get(member);
        return jsonValue != null ?
                jsonValue.asLongRequired() :
                NULL_VALUE.asLongRequired();
    }

    public void putDouble(final String member, final double value) {
        putMember(member, JsonValue.newDouble(value));
    }

    public double getDouble(final String member) {
        final JsonValue jsonValue = membersByName.get(member);
        if (jsonValue == null) {
            return 0d;
        }
        return jsonValue.asDouble();
    }

    public double getDoubleRequired(final String member) {
        final JsonValue jsonValue = membersByName.get(member);
        return jsonValue != null ?
                jsonValue.asDoubleRequired() :
                NULL_VALUE.asDoubleRequired();
    }

    public void putDecimal(final String member, final JsonNumber value) {
        putMember(member, JsonValue.newDecimal(value));
    }

    public void putDecimal(final String member, final BigDecimal value) {
        putMember(member, JsonValue.newDecimal(value));
    }

    public BigDecimal getDecimal(final String member) {
        final JsonValue jsonValue = membersByName.get(member);
        if (jsonValue == null) {
            return JsonValue.ZERO;
        }
        return jsonValue.asDecimal();
    }

    public BigDecimal getDecimalRequired(final String member) {
        final JsonValue jsonValue = membersByName.get(member);
        return jsonValue != null ?
                jsonValue.asDecimalRequired() :
                NULL_VALUE.asDecimalRequired();
    }

    public @Decimal long getDecimal64(final String member) {
        final JsonValue jsonValue = membersByName.get(member);
        if (jsonValue == null) {
            return Decimal64Utils.ZERO;
        }
        return jsonValue.asDecimal64();
    }

    public @Decimal long getDecimal64Required(final String member) {
        final JsonValue jsonValue = membersByName.get(member);
        return jsonValue != null ?
            jsonValue.asDecimal64Required() :
            NULL_VALUE.asDecimal64Required();
    }

    public void putBoolean(final String member, final boolean value) {
        putMember(member, JsonValue.newBoolean(value));
    }

    public boolean getBoolean(final String member) {
        final JsonValue jsonValue = membersByName.get(member);
        if (jsonValue == null) {
            return false;
        }
        return jsonValue.asBoolean();
    }

    public boolean getBooleanRequired(final String member) {
        final JsonValue jsonValue = membersByName.get(member);
        return jsonValue != null ?
                jsonValue.asBooleanRequired() :
                NULL_VALUE.asBooleanRequired();
    }

    public void forAnyObject(BiConsumer<String, JsonObject> consumer) {
        membersByName.entrySet().stream()
                .filter(e -> e.getValue() != null && e.getValue().asObject() != null)
                .limit(1)
                .forEach(e ->
                    consumer.accept(e.getKey(), e.getValue().asObject()));
    }

    public void forEachObject(BiConsumer<String, JsonObject> consumer) {
        membersByName.forEach((key, value) -> {
            if (value == null) {
                return;
            }
            final JsonObject jsonObject = value.asObject();
            if (jsonObject == null) {
                return;
            }
            consumer.accept(key, jsonObject);
        });
    }

    public void forAnyArray(BiConsumer<String, JsonArray> consumer) {
        membersByName.entrySet().stream()
                .filter(e -> e.getValue() != null && e.getValue().asArray() != null)
                .limit(1)
                .forEach(e ->
                    consumer.accept(e.getKey(), e.getValue().asArray()));
    }

    public void forEachArray(BiConsumer<String, JsonArray> consumer) {
        membersByName.forEach((key, value) -> {
            if (value == null) {
                return;
            }
            final JsonArray jsonArray = value.asArray();
            if (jsonArray == null) {
                return;
            }
            consumer.accept(key, jsonArray);
        });
    }

    public void forEachString(BiConsumer<String, String> consumer) {
        membersByName.forEach((key, value) -> {
            if (value != null) {
                final String string = value.asString();
                if (string != null) {
                    consumer.accept(key, string);
                }
            }
        });
    }

    public void putNull(final String member) {
        putMember(member, null);
    }

    public boolean isNull(final String member) {
        final JsonValue jsonValue = membersByName.get(member);
        if (jsonValue == null) {
            return false;
        }
        return jsonValue.isNull();
    }

    public boolean isNullRequired(final String member) {
        final JsonValue jsonValue = membersByName.get(member);
        return jsonValue != null ?
                jsonValue.asBooleanRequired() :
                NULL_VALUE.isNull();
    }

    JsonValue putObjectValue(final String member) {
        return putMember(member, JsonValue.newObject());
    }

    JsonValue putArrayValue(final String member) {
        return putMember(member, JsonValue.newArray());
    }

    private JsonValue putMember(final String member, final JsonValue value) {
        members.add(new Member(member, value));
        membersByName.put(member, value);
        return value;
    }

    public void toJson(final JsonWriter jsonGenerator) {
        jsonGenerator.startObject();
        for (final Member member : members) {
            jsonGenerator.objectMember(member.name);
            member.value.toJson(jsonGenerator);
        }
        jsonGenerator.endObject();
    }

    @Override
    public String toString() {
        final StringBuilder result = new StringBuilder();
        final JsonGenerator generator = new JsonGenerator();
        generator.setOutput(new AppendableWriter<>(result));
        toJson(new JsonWriterToGenerator(generator));
        return result.toString();
    }

    private final class Member {
        private final String name;
        private final JsonValue value;

        private Member(final String name, final JsonValue value) {
            this.name = name;
            this.value = value;
        }
    }
}
