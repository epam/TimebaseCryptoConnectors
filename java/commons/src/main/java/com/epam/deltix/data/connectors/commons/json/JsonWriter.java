package com.epam.deltix.data.connectors.commons.json;

import io.github.green4j.jelly.JsonNumber;

public interface JsonWriter {

    void startObject();

    void objectMember(CharSequence name);

    void objectMember(CharSequence name, int start, int len);

    void endObject();

    void startArray();

    void endArray();

    void stringValue(final CharSequence value, final boolean escaping);

    void stringValue(final CharSequence value);

    void stringValue(final CharSequence name, final int start, final int len, final boolean escaping);

    void stringValue(final CharSequence name, final int start, final int len);

    void numberValueAsString(final JsonNumber value);

    void numberValueAsString(final long value);

    void numberValueAsString(final long mantissa, final int exp);

    void numberValue(final long value);

    void numberValue(final JsonNumber value);

    void numberValue(final long mantissa, final int exp);

    void trueValue();

    void falseValue();

    void nullValue();

    void eoj();
}
