package app.config;

import app.enums.ReportingSetupType;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class ReportingSetupTypeConverter implements AttributeConverter<ReportingSetupType, Long> {

    @Override
    public Long convertToDatabaseColumn(ReportingSetupType attribute) {
        return attribute == null ? null : attribute.getValue();
    }

    @Override
    public ReportingSetupType convertToEntityAttribute(Long dbData) {
        if (dbData == null) {
            return null;
        }

        for (ReportingSetupType type : ReportingSetupType.values()) {
            if (type.getValue().equals(dbData)) {
                return type;
            }
        }

        throw new IllegalArgumentException(
                "Unknown ReportingSetupType value: " + dbData
        );
    }
}