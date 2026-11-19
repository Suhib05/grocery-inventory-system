package com.grocery.web;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.function.Function;

/** Shared Gson instance that reads and writes java.time values as ISO-8601 strings. */
public final class JsonUtil {

    public static final Gson GSON = new GsonBuilder()
            .registerTypeAdapter(LocalDate.class, isoAdapter(LocalDate::parse).nullSafe())
            .registerTypeAdapter(LocalDateTime.class, isoAdapter(LocalDateTime::parse).nullSafe())
            .create();

    private JsonUtil() { }

    private static <T> TypeAdapter<T> isoAdapter(Function<String, T> parser) {
        return new TypeAdapter<>() {
            @Override public void write(JsonWriter out, T value) throws IOException {
                out.value(value.toString());
            }
            @Override public T read(JsonReader in) throws IOException {
                if (in.peek() == JsonToken.STRING) {
                    String s = in.nextString();
                    if (s.isBlank()) return null;
                    try {
                        return parser.apply(s);
                    } catch (DateTimeParseException e) {
                        throw new com.google.gson.JsonParseException("Invalid date: " + s, e);
                    }
                }
                throw new com.google.gson.JsonParseException("Expected a date string");
            }
        };
    }
}
