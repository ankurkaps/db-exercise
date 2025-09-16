package com.demo.common.jaxb.adapters;

import jakarta.xml.bind.annotation.adapters.XmlAdapter;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import static java.time.format.DateTimeFormatter.ISO_INSTANT;

public class LocalDateTimeIso8601XmlAdapter extends XmlAdapter<String, LocalDateTime> {

    @Override
    public LocalDateTime unmarshal(String v) {
        return (v == null) ? null : LocalDateTime.parse(v, ISO_INSTANT);
    }

    @Override
    public String marshal(LocalDateTime v) {
        return (v == null) ? null : ISO_INSTANT.format(v.toInstant(ZoneOffset.UTC));
    }
}
