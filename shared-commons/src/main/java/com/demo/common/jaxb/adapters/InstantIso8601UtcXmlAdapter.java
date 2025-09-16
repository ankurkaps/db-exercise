package com.demo.common.jaxb.adapters;

import jakarta.xml.bind.annotation.adapters.XmlAdapter;
import java.time.Instant;
import java.time.format.DateTimeFormatter;

public class InstantIso8601UtcXmlAdapter extends XmlAdapter<String, Instant> {
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ISO_INSTANT;

    @Override
    public Instant unmarshal(String v) {
        return (v == null) ? null : Instant.parse(v);
    }

    @Override
    public String marshal(Instant v) {
        return (v == null) ? null : FORMATTER.format(v);
    }
}
