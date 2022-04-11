package com.epam.deltix.data.connectors.commons.json;

import io.github.green4j.jelly.JsonNumber;
import io.github.green4j.jelly.JsonParser;
import io.github.green4j.jelly.JsonParserListener;

import java.util.LinkedList;

public class JsonValueParser {
    private final JsonParser parser = new JsonParser();
    private final LinkedList<JsonValue> stack = new LinkedList<>();

    private JsonValue currentValue;
    private String currentMember;

    private boolean newJson = true;

    public JsonValueParser() {
        parser.setListener(new JsonDeserializer());
    }

    public void parse(final CharSequence json) {
        if (newJson) {
            stack.clear();
            currentValue = null;
            currentMember = null;
            newJson = false;
        }
        parser.parse(json);

        if (parser.hasError()) {
            throw new IllegalArgumentException("An error '" + parser.getError() +
                    "' as position: " + parser.getErrorPosition());
        }
    }

    public JsonValue eoj() {
        newJson = true;

        parser.eoj();
        return currentValue;
    }

    public JsonValue parseAndEoj(final CharSequence json) {
        parse(json);
        return eoj();
    }

    private class JsonDeserializer implements JsonParserListener {
        @Override
        public void onJsonStarted() {
        }

        @Override
        public void onError(final String error, final int position) {
            throw new IllegalArgumentException("An error " + error + " while parsing");
        }

        @Override
        public void onJsonEnded() {
        }

        @Override
        public boolean onObjectStarted() {
            if (currentValue == null) {
                currentValue = JsonValue.newObject();
                return true;
            }

            stack.push(currentValue);

            final Object value = currentValue.value();

            if (value instanceof JsonObject) {
                currentValue = ((JsonObject) value).putObjectValue(currentMember);
                return true;
            }
            if (value instanceof JsonArray) {
                currentValue = ((JsonArray) value).addObjectValue();
                return true;
            }

            throw new IllegalStateException("Object isn't allowed here");
        }

        @Override
        public boolean onObjectMember(final CharSequence name) {
            currentMember = name.toString();
            return true;
        }

        @Override
        public boolean onObjectEnded() {
            if (!stack.isEmpty()) {
                currentValue = stack.pop();
            }
            currentMember = null;
            return true;
        }

        @Override
        public boolean onArrayStarted() {
            if (currentValue == null) {
                currentValue = JsonValue.newArray();
                return true;
            }

            stack.push(currentValue);

            final Object value = currentValue.value();

            if (value instanceof JsonObject) {
                currentValue = ((JsonObject) value).putArrayValue(currentMember);
                return true;
            }
            if (value instanceof JsonArray) {
                currentValue = ((JsonArray) value).addArrayValue();
                return true;
            }

            throw new IllegalStateException("Array isn't allowed here");
        }

        @Override
        public boolean onArrayEnded() {
            if (!stack.isEmpty()) {
                currentValue = stack.pop();
            }
            return true;
        }

        @Override
        public boolean onStringValue(final CharSequence data) {
            final String string = data.toString();
            if (currentValue == null) {
                currentValue = JsonValue.newString(string);
                return true;
            }

            final Object value = currentValue.value();

            if (value instanceof JsonObject) {
                ((JsonObject) value).putString(currentMember, string);
                return true;
            }
            if (value instanceof JsonArray) {
                ((JsonArray) value).addString(string);
                return true;
            }

            throw new IllegalStateException("Scalar string " + string + " isn't allowed here");
        }

        @Override
        public boolean onNumberValue(final JsonNumber number) {
            if (currentValue == null) {
                currentValue = JsonValue.newDecimal(number);
                return true;
            }

            final Object value = currentValue.value();

            if (value instanceof JsonObject) {
                ((JsonObject) value).putDecimal(currentMember, number);
                return true;
            }
            if (value instanceof JsonArray) {
                ((JsonArray) value).addDecimal(number);
                return true;
            }

            throw new IllegalStateException("Scalar number " + number + " isn't allowed here");
        }

        @Override
        public boolean onTrueValue() {
            onBoolean(true);
            return true;
        }

        @Override
        public boolean onFalseValue() {
            onBoolean(false);
            return true;
        }

        private void onBoolean(final boolean v) {
            if (currentValue == null) {
                currentValue = JsonValue.newBoolean(v);
            }

            final Object value = currentValue.value();

            if (value instanceof JsonObject) {
                ((JsonObject) value).putBoolean(currentMember, v);
                return;
            }
            if (value instanceof JsonArray) {
                ((JsonArray) value).addBoolean(v);
                return;
            }

            throw new IllegalStateException("Scalar boolean " + v + " isn't allowed here");
        }


        @Override
        public boolean onNullValue() {
            if (currentValue == null) {
                currentValue = JsonValue.newNull();
                return true;
            }

            final Object value = currentValue.value();

            if (value instanceof JsonObject) {
                ((JsonObject) value).putNull(currentMember);
                return true;
            }
            if (value instanceof JsonArray) {
                ((JsonArray) value).addNull();
                return true;
            }

            throw new IllegalStateException("Scalar null isn't allowed here");
        }
    }
}
