package com.qcadoo.helpers;

import java.io.File;

import org.apache.commons.lang3.StringUtils;

import com.qcadoo.dtos.TranslationPosition;

public class MissingTranslationsHelper {

    public static final String L_DIRECTORY = "/Users/username/";

    public static final String L_DOT = ".";

    public static final String L_PROPERTIES = "properties";

    public static final String L_SLASH = "/";

    public static final String L_CSV = "csv";

    public static final String L_XLSX = "xlsx";

    public static final String L_EXPORTED_CSV_SEPARATOR = "','";

    public static final String L_QUOTE = "\"";

    public static final String L_N = "\n";

    public static String getDirectoryName(final String pathname, final String project) {
        return new StringBuilder(L_DIRECTORY).append(pathname).append(L_SLASH).append(project).append(L_SLASH).toString();
    }

    public static String getPropertiesLanguageAndExtension(final String language) {
        return new StringBuilder(language).append(L_DOT).append(L_PROPERTIES).toString();
    }

    public static File getTranslationsFile(final String project, final String extension) {
        File directory = new File(MissingTranslationsHelper.L_DIRECTORY);

        directory.mkdirs();

        return new File(directory, getTranslationsFileName(project, extension));
    }

    public static String getTranslationsFileName(final String project, final String extension) {
        return new StringBuilder().append(project).append(L_DOT).append(extension).toString();
    }

    public static TranslationPosition createTranslationPosition(final String path, final String key, final String sourceValue,
            final String targetValue) {
        TranslationPosition position = new TranslationPosition();

        position.setPath(path);
        position.setKey(key);
        position.setSourceValue(sourceValue);
        position.setTargetValue(targetValue);

        return position;
    }

    public static TranslationPosition updateTranslationPosition(final TranslationPosition position, final String targetValue) {
        position.setTargetValue(targetValue);

        return position;
    }

    public static String normalizeString(final String string) {
        if (StringUtils.isNotBlank(string)) {
            return string.replaceAll(L_QUOTE, "\\\"").replaceAll(L_N, " ");
        } else {
            return "";
        }
    }

}
