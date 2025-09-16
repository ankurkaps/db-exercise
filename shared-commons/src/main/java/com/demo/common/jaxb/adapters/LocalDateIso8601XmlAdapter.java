package com.demo.common.jaxb.adapters;

import jakarta.xml.bind.annotation.adapters.XmlAdapter;
import java.time.LocalDate;
import static java.time.format.DateTimeFormatter.ISO_LOCAL_DATE;

public class LocalDateIso8601XmlAdapter extends XmlAdapter<String, LocalDate> {
    @Override
    public LocalDate unmarshal(String v) {
        return (v == null) ? null : LocalDate.parse(v, ISO_LOCAL_DATE);
    }

    @Override
    public String marshal(LocalDate v) {
        return (v == null) ? null : ISO_LOCAL_DATE.format(v);
    }
}
