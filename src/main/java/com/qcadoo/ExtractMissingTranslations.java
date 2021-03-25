package com.qcadoo;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;

import com.google.common.collect.Lists;
import com.qcadoo.dto.TranslationPosition;

public class ExtractMissingTranslations {

    private static final String L_DIRECTORY = "/Users/username/";

    private static final String L_DOT = ".";

    private static final String L_PROPERTIES = "properties";

    private static final String L_EXPORTED_CSV_SEPARATOR = "','";

    public static final String L_SLASH = "/";

    public static void main(final String[] args) throws IOException {
        String pathname = "translations";
        String project = "qcadoo";
        String fromLanguage = "en";
        String toLanguage = "cn";

        String directoryName = getDirectoryName(pathname, project);

        File directory = new File(directoryName);

        Collection<File> files = FileUtils.listFiles(directory, new String[] { L_PROPERTIES }, true);

        List<TranslationPosition> positions = Lists.newArrayList();

        for (File file : files) {
            String fileName = file.getName().toLowerCase();

            if (fileName.endsWith(getPropertiesLanguageAndExtension(fromLanguage))) {
                String path = file.getPath().replace(directoryName, L_SLASH);

                BufferedReader source = new BufferedReader(new FileReader(file));

                Optional<File> mayBeTargetFile = getTargetFile(files, fileName, fromLanguage, toLanguage);

                if (mayBeTargetFile.isPresent()) {
                    BufferedReader target = new BufferedReader(new FileReader(mayBeTargetFile.get()));

                    addTranslationPositions(source, positions, path);
                    updateTranslationPositions(target, positions, path);
                } else {
                    addTranslationPositions(source, positions, path);
                }
            }
        }

        exportTranslationPositions(positions, project, fromLanguage, toLanguage);
    }

    private static Optional<File> getTargetFile(final Collection<File> files, final String fileName, final String fromLanguage,
            final String toLanguage) {
        String fromLanguageAndExtension = getPropertiesLanguageAndExtension(fromLanguage);
        String toLanguageAndExtension = getPropertiesLanguageAndExtension(toLanguage);

        return files.stream().filter(
                file -> file.getName().toLowerCase().equals(fileName.replace(fromLanguageAndExtension, toLanguageAndExtension)))
                .findAny();
    }

    private static String getDirectoryName(final String pathname, final String project) {
        return new StringBuilder(L_DIRECTORY).append(pathname).append(L_SLASH).append(project).append(L_SLASH).toString();
    }

    private static String getPropertiesLanguageAndExtension(final String language) {
        return new StringBuilder(language).append(L_DOT).append(L_PROPERTIES).toString();
    }

    private static void addTranslationPositions(final BufferedReader source, final List<TranslationPosition> positions,
            final String path) throws IOException {
        String line;

        while (Objects.nonNull(line = source.readLine())) {
            int index = line.indexOf("=");

            if (index > 0) {
                String key = line.substring(0, index).trim();
                String value = line.substring(index + 1).trim();

                TranslationPosition position = createTranslationPosition(path, key, value, "");

                positions.add(position);
            }
        }
    }

    private static void updateTranslationPositions(final BufferedReader target, final List<TranslationPosition> positions,
            final String path) throws IOException {
        String line;

        while (Objects.nonNull(line = target.readLine())) {
            int index = line.indexOf("=");

            if (index > 0) {
                String key = line.substring(0, index).trim();
                String value = line.substring(index + 1).trim();

                Optional<TranslationPosition> mayBePosition = positions.stream().filter(position -> position.getKey().equals(key))
                        .findFirst();

                if (mayBePosition.isPresent()) {
                    TranslationPosition position = updateTranslationPosition(mayBePosition.get(), value);
                } else {
                    TranslationPosition position = createTranslationPosition(path, key, "", value);

                    positions.add(position);
                }
            }
        }
    }

    private static TranslationPosition createTranslationPosition(final String path, final String key, final String sourceValue,
            final String targetValue) {
        TranslationPosition position = new TranslationPosition();

        position.setPath(path);
        position.setKey(key);
        position.setSourceValue(sourceValue);
        position.setTargetValue(targetValue);

        return position;
    }

    private static TranslationPosition updateTranslationPosition(final TranslationPosition position, final String targetValue) {
        position.setTargetValue(targetValue);

        return position;
    }

    private static void exportTranslationPositions(final List<TranslationPosition> positions, final String project,
            final String fromLanguage, final String toLanguage) {
        positions.forEach(position -> System.out.println(position.toString()));

        File file = getExportFileName(project);

        BufferedWriter bufferedWriter = null;

        try (FileOutputStream fileOutputStream = new FileOutputStream(file)) {
            fileOutputStream.write(239);
            fileOutputStream.write(187);
            fileOutputStream.write(191);

            bufferedWriter = new BufferedWriter(new OutputStreamWriter(fileOutputStream, StandardCharsets.UTF_8));

            createRow(bufferedWriter, "Path", "Key", fromLanguage.toUpperCase(), toLanguage.toUpperCase());

            for (TranslationPosition position : positions) {
                createRow(bufferedWriter, position.getPath(), position.getKey(), position.getSourceValue(),
                        position.getTargetValue());
            }

            bufferedWriter.flush();
        } catch (IOException e) {
            throw new IllegalStateException(e.getMessage(), e);
        } finally {
            IOUtils.closeQuietly(bufferedWriter);
        }
    }

    private static File getExportFileName(final String project) {
        String date = Long.toString(System.currentTimeMillis());

        File directory = new File(L_DIRECTORY);

        return new File(directory, getTranslationsFileName(project, date));
    }

    private static String getTranslationsFileName(final String project, final String date) {
        return new StringBuilder("translations").append("_").append(project).append("_").append(date).append(".csv").toString();
    }

    private static void createRow(final BufferedWriter bufferedWriter, final String path, final String key,
            final String sourceValue, final String targetValue) throws IOException {
        bufferedWriter.append("\"").append(normalizeString(path)).append("\"");
        bufferedWriter.append(L_EXPORTED_CSV_SEPARATOR);
        bufferedWriter.append("\"").append(normalizeString(key)).append("\"");
        bufferedWriter.append(L_EXPORTED_CSV_SEPARATOR);
        bufferedWriter.append("\"").append(normalizeString(sourceValue)).append("\"");
        bufferedWriter.append(L_EXPORTED_CSV_SEPARATOR);
        bufferedWriter.append("\"").append(normalizeString(targetValue)).append("\"");
        bufferedWriter.append(L_EXPORTED_CSV_SEPARATOR);
        bufferedWriter.append("\n");
    }

    private static String normalizeString(final String string) {
        if (StringUtils.isNotBlank(string)) {
            return string.replaceAll("\"", "\\\"").replaceAll("\n", " ");
        } else {
            return "";
        }
    }

}
