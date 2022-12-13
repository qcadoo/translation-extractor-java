package com.qcadoo;

import com.google.common.collect.Lists;
import com.qcadoo.dtos.TranslationPosition;
import com.qcadoo.helpers.MissingTranslationsHelper;
import org.apache.commons.io.FileUtils;
import org.apache.poi.hssf.usermodel.HSSFFont;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.*;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class ExportMissingTranslations {

    public static void main(final String[] args) throws IOException {
        System.out.println("Creating translations for: " + args[1]);

        String pathname = args[0];
        String project = args[1];
        String fromLanguage = "en";
        String toLanguage = "cn";

        exportTranslationPositions(pathname, project, fromLanguage, toLanguage,
                getTranslationPositions(pathname, project, fromLanguage, toLanguage));
    }

    private static void exportTranslationPositions(final String pathname, final String project, final String fromLanguage, final String toLanguage,
                                                       final List<TranslationPosition> positions) {
        File file = MissingTranslationsHelper.getTranslationsFile(pathname, project, MissingTranslationsHelper.L_XLSX);

        try (FileOutputStream fileOutputStream = new FileOutputStream(file)) {
            XSSFWorkbook workbook = new XSSFWorkbook();

            XSSFSheet sheet = workbook.createSheet("Translations");

            setColumnsWidths(sheet);

            createHeaderRow(workbook, sheet, fromLanguage, toLanguage);

            int rowNum = 1;

            for (TranslationPosition position : positions) {
                createRow(workbook, sheet, rowNum, position);

                rowNum++;
            }

            workbook.write(fileOutputStream);
        } catch (IOException e) {
            throw new IllegalStateException(e.getMessage(), e);
        }
    }

    private static void setColumnsWidths(final XSSFSheet sheet) {
        sheet.setColumnWidth(0, 25000);
        sheet.setColumnWidth(1, 25000);
        sheet.setColumnWidth(2, 10000);
        sheet.setColumnWidth(3, 10000);
    }

    private static void createHeaderRow(final XSSFWorkbook workbook, final XSSFSheet sheet, final String fromLanguage, final String toLanguage) {
        XSSFRow dataHeaderRow = sheet.createRow(0);

        XSSFCellStyle styleBold = workbook.createCellStyle();

        styleBold.setFont(createFontArialHeight10AndBold(workbook));

        styleBold.setWrapText(false);
        styleBold.setAlignment(HorizontalAlignment.CENTER);
        styleBold.setVerticalAlignment(VerticalAlignment.CENTER);

        int columnIndex = 0;

        for (String value : Lists.newArrayList("Path", "Key", fromLanguage.toUpperCase(), toLanguage.toUpperCase())) {
            dataHeaderRow.createCell(columnIndex).setCellValue(value);
            dataHeaderRow.getCell(columnIndex).setCellStyle(styleBold);

            columnIndex++;
        }
    }

    private static void createRow(final XSSFWorkbook workbook, final XSSFSheet sheet, int rowNum, final TranslationPosition position) {
        XSSFRow row = sheet.createRow(rowNum);

        XSSFCellStyle styleBold = workbook.createCellStyle();

        styleBold.setFont(createFontArialHeight10AndNormal(workbook));

        styleBold.setWrapText(true);
        styleBold.setAlignment(HorizontalAlignment.LEFT);
        styleBold.setVerticalAlignment(VerticalAlignment.CENTER);

        int columnIndex = 0;

        for (String value : Lists.newArrayList(position.getPath(), position.getKey(), position.getSourceValue(), position.getTargetValue())) {
            row.createCell(columnIndex).setCellValue(value);
            row.getCell(columnIndex).setCellStyle(styleBold);

            columnIndex++;
        }
    }

    private static Font createFontArialHeight10AndBold(final XSSFWorkbook workbook) {
        return createFontArialWithGivenFontHeightAndBoldWeight(workbook, (short) 10, true);
    }

    private static Font createFontArialHeight10AndNormal(final XSSFWorkbook workbook) {
        return createFontArialWithGivenFontHeightAndBoldWeight(workbook, (short) 10, false);
    }

    private static Font createFontArialWithGivenFontHeightAndBoldWeight(final XSSFWorkbook workbook, final short fontHeight, final boolean bold) {
        Font arialFont = workbook.createFont();

        arialFont.setFontName(HSSFFont.FONT_ARIAL);
        arialFont.setFontHeightInPoints(fontHeight);
        arialFont.setBold(bold);

        return arialFont;
    }

    private static List<TranslationPosition> getTranslationPositions(final String pathname, final String project,
                                                                     final String fromLanguage, final String toLanguage) throws IOException {
        String directoryName = MissingTranslationsHelper.getDirectoryName(pathname, project);

        File directory = new File(directoryName);

        Collection<File> files = FileUtils.listFiles(directory, new String[]{MissingTranslationsHelper.L_PROPERTIES}, true);

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
