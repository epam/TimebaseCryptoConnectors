package com.epam.deltix.data.connectors.commons.json;

import io.github.green4j.jelly.JsonGenerator;
import io.github.green4j.jelly.JsonNumber;

public class JsonWriterToGenerator implements JsonWriter {
    private final JsonGenerator generator;

    public JsonWriterToGenerator(final JsonGenerator generator) {
        this.generator = generator;
    }

    @Override
    public void startObject() {
        generator.startObject();
    }

    @Override
    public void objectMember(final CharSequence name) {
        generator.objectMember(name);
    }

    @Override
    public void objectMember(final CharSequence name, final int start, final int len) {
        generator.objectMember(name, start, len);
    }

    @Override
    public void endObject() {
        generator.endObject();
    }

    @Override
    public void startArray() {
        generator.startArray();
    }

    @Override
    public void endArray() {
        generator.endArray();
    }

    @Override
    public void stringValue(final CharSequence value, final boolean escaping) {
        generator.stringValue(value, escaping);
    }

    @Override
    public void stringValue(final CharSequence value) {
        generator.stringValue(value);
    }

    @Override
    public void stringValue(final CharSequence name, final int start, final int len, final boolean escaping) {
        generator.stringValue( name, start, len, escaping);
    }

    @Override
    public void stringValue(final CharSequence name, final int start, final int len) {
        generator.stringValue(name, start, len);
    }

    @Override
    public void numberValueAsString(final JsonNumber value) {
        generator.numberValueAsString(value);
    }

    @Override
    public void numberValueAsString(final long value) {
        generator.numberValueAsString(value);
    }

    @Override
    public void numberValueAsString(final long mantissa, final int exp) {
        generator.numberValueAsString(mantissa, exp);
    }

    @Override
    public void numberValue(final long value) {
        generator.numberValue(value);
    }

    @Override
    public void numberValue(final JsonNumber value) {
        generator.numberValue(value);
    }

    @Override
    public void numberValue(final long mantissa, final int exp) {
        generator.numberValue(mantissa, exp);
    }

    @Override
    public void trueValue() {
        generator.trueValue();
    }

    @Override
    public void falseValue() {
        generator.falseValue();
    }

    @Override
    public void nullValue() {
        generator.nullValue();
    }

    @Override
    public void eoj() {
        generator.eoj();
    }
}
