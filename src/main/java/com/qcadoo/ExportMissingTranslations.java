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

import com.google.common.collect.Lists;
import com.qcadoo.dtos.TranslationPosition;
import com.qcadoo.helpers.MissingTranslationsHelper;

public class ExportMissingTranslations {

    public static void main(final String[] args) throws IOException {
        String pathname = "translations";
        String project = "qcadoo";
        String fromLanguage = "en";
        String toLanguage = "cn";

        exportTranslationPositions(project, fromLanguage, toLanguage,
                getTranslationPositions(pathname, project, fromLanguage, toLanguage));
    }

    private static void exportTranslationPositions(final String project, final String fromLanguage, final String toLanguage,
            final List<TranslationPosition> positions) {
        File file = MissingTranslationsHelper.getTranslationsFile(project, MissingTranslationsHelper.L_CSV);

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

    private static void createRow(final BufferedWriter bufferedWriter, final String path, final String key,
            final String sourceValue, final String targetValue) throws IOException {
        bufferedWriter.append(MissingTranslationsHelper.L_QUOTE).append(MissingTranslationsHelper.normalizeString(path))
                .append(MissingTranslationsHelper.L_QUOTE);
        bufferedWriter.append(MissingTranslationsHelper.L_EXPORTED_CSV_SEPARATOR);
        bufferedWriter.append(MissingTranslationsHelper.L_QUOTE).append(MissingTranslationsHelper.normalizeString(key))
                .append(MissingTranslationsHelper.L_QUOTE);
        bufferedWriter.append(MissingTranslationsHelper.L_EXPORTED_CSV_SEPARATOR);
        bufferedWriter.append(MissingTranslationsHelper.L_QUOTE).append(MissingTranslationsHelper.normalizeString(sourceValue))
                .append(MissingTranslationsHelper.L_QUOTE);
        bufferedWriter.append(MissingTranslationsHelper.L_EXPORTED_CSV_SEPARATOR);
        bufferedWriter.append(MissingTranslationsHelper.L_QUOTE).append(MissingTranslationsHelper.normalizeString(targetValue))
                .append(MissingTranslationsHelper.L_QUOTE);
        bufferedWriter.append(MissingTranslationsHelper.L_EXPORTED_CSV_SEPARATOR);
        bufferedWriter.append("\n");
    }

    private static List<TranslationPosition> getTranslationPositions(final String pathname, final String project,
            final String fromLanguage, final String toLanguage) throws IOException {
        String directoryName = MissingTranslationsHelper.getDirectoryName(pathname, project);

        File directory = new File(directoryName);

        Collection<File> files = FileUtils.listFiles(directory, new String[] { MissingTranslationsHelper.L_PROPERTIES }, true);

        List<TranslationPosition> positions = Lists.newArrayList();

        for (File file : files) {
            String fileName = file.getName().toLowerCase();

            if (fileName.endsWith(MissingTranslationsHelper.getPropertiesLanguageAndExtension(fromLanguage))) {
                String path = file.getPath().replace(directoryName, MissingTranslationsHelper.L_SLASH);

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

        return positions;
    }

    private static Optional<File> getTargetFile(final Collection<File> files, final String fileName, final String fromLanguage,
            final String toLanguage) {
        String fromLanguageAndExtension = MissingTranslationsHelper.getPropertiesLanguageAndExtension(fromLanguage);
        String toLanguageAndExtension = MissingTranslationsHelper.getPropertiesLanguageAndExtension(toLanguage);

        return files.stream().filter(
                file -> file.getName().toLowerCase().equals(fileName.replace(fromLanguageAndExtension, toLanguageAndExtension)))
                .findAny();
    }

    private static void addTranslationPositions(final BufferedReader source, final List<TranslationPosition> positions,
            final String path) throws IOException {
        String line;

        while (Objects.nonNull(line = source.readLine())) {
            int index = line.indexOf("=");

            if (index > 0) {
                String key = line.substring(0, index).trim();
                String value = line.substring(index + 1).trim();

                TranslationPosition position = MissingTranslationsHelper.createTranslationPosition(path, key, value, "");

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
                    TranslationPosition position = MissingTranslationsHelper.updateTranslationPosition(mayBePosition.get(),
                            value);
                } else {
                    TranslationPosition position = MissingTranslationsHelper.createTranslationPosition(path, key, "", value);

                    positions.add(position);
                }
            }
        }
    }

}
