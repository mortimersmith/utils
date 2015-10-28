package com.github.mortimersmith.utils;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public final class Utils
{
    private Utils() {}

    public interface ThrowingRunnable<E extends Exception> { void run() throws E; }
    public interface ThrowingSupplier<T, E extends Exception> { T get() throws E; }
    public interface ThrowingConsumer<T, E extends Exception> { void accept(T t) throws E; }
    public interface ThrowingBiConsumer<T, U, E extends Exception> { void accept(T t, U u) throws E; }

    public static <T extends Closeable, E extends IOException>
        void with(ThrowingSupplier<T, E> s, ThrowingConsumer<T, E> c)
            throws IOException
    {
        try(T t = s.get()) { c.accept(t); }
    }

    public static <T extends Closeable, U extends Closeable, E extends IOException>
        void with(ThrowingSupplier<T, E> st, ThrowingSupplier<U, E> su, ThrowingBiConsumer<T, U, E> c)
            throws IOException
    {
        try(T t = st.get()) { try(U u = su.get()) { c.accept(t, u); } }
    }

    public static String read(InputStream in, Charset c) throws IOException
    {
        try (BufferedReader r = new BufferedReader(new InputStreamReader(in, c))) {
            return r.lines().collect(Collectors.joining());
        }
    }

    public static <T, U> Consumer<Map.Entry<T, U>> split(BiConsumer<T, U> c) {
        return (e) -> c.accept(e.getKey(), e.getValue());
    }

    public static <T, U, E extends Exception> ThrowingConsumer<Map.Entry<T, U>, E> splitE(ThrowingBiConsumer<T, U, E> c) throws E {
        return (e) -> c.accept(e.getKey(), e.getValue());
    }

    public static <T, E extends Exception> void forEachE(Iterable<T> i, ThrowingConsumer<T, E> c) throws E
    {
        for (T t : i) c.accept(t);
    }

    public static <T, U, E extends Exception> void forEachE(Map<T, U> m, ThrowingBiConsumer<T, U, E> c) throws E
    {
        for (Map.Entry<T, U> e : m.entrySet()) splitE(c).accept(e);
    }

    public interface OtherwiseE<E extends Exception>
    {
        public void otherwise(ThrowingRunnable<E> r) throws E;
    }

    public static <T, E extends Exception> OtherwiseE<E> ifPresentE(Optional<T> o, ThrowingConsumer<T, E> c) throws E
    {
        if (o.isPresent()) { c.accept(o.get()); return ifPresentOtherwiseE(true); }
        else return ifPresentOtherwiseE(false);
    }

    private static <E extends Exception> OtherwiseE<E> ifPresentOtherwiseE(boolean wasPresent)
    {
        return (r) -> { if (!wasPresent) r.run(); };
    }
}
