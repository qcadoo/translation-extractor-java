package com.qcadoo.dtos;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

public class TranslationPosition {

    private String path;

    private String key;

    private String sourceValue;

    private String targetValue;

    public String getPath() {
        return path;
    }

    public void setPath(final String path) {
        this.path = path;
    }

    public String getKey() {
        return key;
    }

    public void setKey(final String key) {
        this.key = key;
    }

    public String getSourceValue() {
        return sourceValue;
    }

    public void setSourceValue(final String sourceValue) {
        this.sourceValue = sourceValue;
    }

    public String getTargetValue() {
        return targetValue;
    }

    public void setTargetValue(final String targetValue) {
        this.targetValue = targetValue;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        TranslationPosition that = (TranslationPosition) o;

        return new EqualsBuilder().append(path, that.path).append(key, that.key)
                .append(sourceValue, that.sourceValue).append(targetValue, that.targetValue).isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().append(path).append(key).append(sourceValue).append(targetValue).toHashCode();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append("path", path)
                .append("key", key)
                .append("sourceValue", sourceValue)
                .append("targetValue", targetValue)
                .toString();
    }

}
