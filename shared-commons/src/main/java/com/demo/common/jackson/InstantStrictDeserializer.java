package com.demo.common.jackson;

import java.io.IOException;
import java.time.Instant;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

/**
 * Deserialize Instant only from strings matching yyyy-MM-dd'T'HH:mm:ssZ (no fractional seconds).
 * Throws JsonParseException with a clear message on mismatch.
 */
public class InstantStrictDeserializer extends JsonDeserializer<Instant> {

    private static final String STRICT_REGEX = "^[0-9]{4}-[0-9]{2}-[0-9]{2}T[0-9]{2}:[0-9]{2}:[0-9]{2}Z$";

    @Override
    public Instant deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        String text = p.getText();
        if (text == null) {
            return null;
        }
        if (!text.matches(STRICT_REGEX)) {
            throw new JsonParseException(p, "creationTimestamp must be in format yyyy-MM-dd'T'HH:mm:ssZ (UTC, no fractional seconds). Got: " + text);
        }
        try {
            return Instant.parse(text); // ISO_INSTANT accepts the exact format with trailing Z
        } catch (Exception e) {
            throw new JsonParseException(p, "Failed to parse creationTimestamp: " + e.getMessage(), e);
        }
    }
}