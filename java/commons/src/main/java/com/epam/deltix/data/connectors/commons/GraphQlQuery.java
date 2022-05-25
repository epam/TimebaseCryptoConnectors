package com.epam.deltix.data.connectors.commons;

import com.epam.deltix.data.connectors.commons.Util;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class GraphQlQuery {
    public static Query query(final String type) {
        return new Query(type);
    }

    public static class Arguments {
        private int first = Integer.MIN_VALUE;
        private int skip = Integer.MIN_VALUE;
        private String orderBy;

        private Arguments() {
        }

        public Arguments withFirst(final int first) {
            this.first = first;
            return this;
        }

        public Arguments withNoFirst() {
            this.first = Integer.MIN_VALUE;
            return this;
        }

        public Arguments withSkip(final int skip) {
            this.skip = skip;
            return this;
        }

        public Arguments withNoSkip() {
            this.skip = Integer.MIN_VALUE;
            return this;
        }

        public Arguments withOrderBy(final String orderBy) {
            this.orderBy = orderBy;
            return this;
        }

        private boolean hasFirst() {
            return first != Integer.MIN_VALUE;
        }

        private boolean hasSkip() {
            return skip != Integer.MIN_VALUE;
        }

        private boolean hasOrderBy() {
            return orderBy != null;
        }

        private boolean hasArguments() {
            return  hasFirst() || hasSkip() || hasOrderBy();
        }

        void write(final Appendable to) throws IOException {
            if (!hasArguments()) {
                return;
            }

            to.append('(');

            int added = 0;

            if (hasFirst()) {
                to.append("first: ").append(Integer.toString(first));
                added++;
            }

            if (hasSkip()) {
                if (added > 0) {
                    to.append(", ");
                }
                to.append("skip: ").append(Integer.toString(skip));
                added++;
            }

            if (hasOrderBy()) {
                if (added > 0) {
                    to.append(", ");
                }
                to.append("orderBy: ").append(orderBy);
            }

            to.append(')');
        }
    }

    public static abstract class Field {
        protected final String name;

        protected Field(final String name) {
            this.name = name;
        }

        abstract void write(Appendable to, int level) throws IOException;
    }

    private static class Scalar extends Field {
        private Scalar(final String name) {
            super(name);
        }

        @Override
        void write(final Appendable to, final int level) throws IOException {
            Util.tabBy2(to, level);
            to.append(name);
            to.append(Util.NATIVE_LINE_BREAK);
        }
    }

    public static class Object extends Field {
        private final Arguments arguments = new Arguments();
        private final List<Field> fields = new ArrayList<>();

        private Object(final String name) {
            super(name);
        }

        public void withScalar(final String name) {
            fields.add(new Scalar(name));
        }

        public Object withObject(final String name) {
            final Object result = new Object(name);
            fields.add(result);
            return result;
        }

        public Arguments arguments() {
            return arguments;
        }

        @Override
        void write(final Appendable to, final int level) throws IOException {
            Util.tabBy2(to, level);
            to.append(name);
            arguments.write(to);
            to.append(' ');
            to.append('{').append(Util.NATIVE_LINE_BREAK);
            for (Field f : fields) {
                f.write(to, level + 1);
            }
            Util.tabBy2(to, level);
            to.append('}').append(Util.NATIVE_LINE_BREAK);
        }
    }

    public static class Query extends Object {
        private Query(final String name) {
            super(name);
        }

        public void write(final Appendable to) throws IOException {
            to.append('{').append(Util.NATIVE_LINE_BREAK);
            write(to, 1);
            to.append('}');
        }

        public void writeJson(final Appendable to) throws IOException {
            to.append("{\"query\": \"");

            write(new Appendable() {
                @Override
                public Appendable append(final CharSequence csq) throws IOException {
                    return append(csq, 0, csq.length());
                }

                @Override
                public Appendable append(final CharSequence csq, final int start, final int end) throws IOException {
                    for (int i = start; i < end; i++) {
                        append(csq.charAt(i));
                    }
                    return this;
                }

                @Override
                public Appendable append(final char c) throws IOException {
                    switch (c) {
                        case '\r':
                            to.append("\\r");
                            break;
                        case '\n':
                            to.append("\\n");
                            break;
                        default:
                            to.append(c);
                    }
                    return this;
                }
            });

            to.append("\"}");
        }
    }

    private GraphQlQuery() {
    }
}
