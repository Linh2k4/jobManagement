package com.company.jobmanagement.model.converter;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.time.YearMonth;
import java.time.format.DateTimeFormatter;

/**
 * Maps {@link YearMonth} to the "yyyy-MM" VARCHAR(7) column format used across
 * the KPI/Evaluation migrations. Without an explicit converter, Hibernate falls
 * back to serializing YearMonth as bytea, which cannot be compared against the
 * varchar columns the schema actually declares.
 */
@Converter(autoApply = true)
public class YearMonthAttributeConverter implements AttributeConverter<YearMonth, String> {

    private static final DateTimeFormatter FORMAT = DateTimeFormatter.ofPattern("yyyy-MM");

    @Override
    public String convertToDatabaseColumn(YearMonth attribute) {
        return attribute == null ? null : attribute.format(FORMAT);
    }

    @Override
    public YearMonth convertToEntityAttribute(String dbData) {
        return dbData == null ? null : YearMonth.parse(dbData, FORMAT);
    }
}
