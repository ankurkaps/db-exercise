@jakarta.xml.bind.annotation.adapters.XmlJavaTypeAdapters({
    @jakarta.xml.bind.annotation.adapters.XmlJavaTypeAdapter(
        type = java.time.Instant.class,
        value = com.demo.common.jaxb.adapters.InstantIso8601UtcXmlAdapter.class
    ),
    @jakarta.xml.bind.annotation.adapters.XmlJavaTypeAdapter(
        type = java.time.LocalDateTime.class,
        value = LocalDateTimeIso8601XmlAdapter.class
    ),
        @jakarta.xml.bind.annotation.adapters.XmlJavaTypeAdapter(
        type = java.time.LocalDate.class,
        value = LocalDateIso8601XmlAdapter.class
    )
})
package com.demo.common.model;

import com.demo.common.jaxb.adapters.LocalDateIso8601XmlAdapter;
import com.demo.common.jaxb.adapters.LocalDateTimeIso8601XmlAdapter;
