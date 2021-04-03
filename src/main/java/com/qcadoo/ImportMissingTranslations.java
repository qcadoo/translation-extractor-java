package com.qcadoo;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.apache.commons.io.IOUtils;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import com.google.common.collect.Lists;
import com.qcadoo.dtos.TranslationPosition;
import com.qcadoo.helpers.MissingTranslationsHelper;

public class ImportMissingTranslations {

    public static void main(final String[] args) throws IOException {
        String pathname = "translations";
        String project = "qcadoo";
        String fromLanguage = "en";
        String toLanguage = "cn";

        importTranslationPositions(pathname, project, fromLanguage, toLanguage,
                getTranslationPositions(project, fromLanguage, toLanguage));
    }

    private static void importTranslationPositions(final String pathname, final String project, final String fromLanguage,
            final String toLanguage, final Map<String, List<TranslationPosition>> positions) {
        String directoryName = MissingTranslationsHelper.getDirectoryName(pathname, project);

        for (Map.Entry<String, List<TranslationPosition>> entry : positions.entrySet()) {
            String path = entry.getKey();

            String fromLanguageAndExtension = MissingTranslationsHelper.getPropertiesLanguageAndExtension(fromLanguage);
            String toLanguageAndExtension = MissingTranslationsHelper.getPropertiesLanguageAndExtension(toLanguage);

            String fileName = path.replace(fromLanguageAndExtension, toLanguageAndExtension);

            File file = new File(directoryName, fileName);

            BufferedWriter bufferedWriter = null;

            try (FileOutputStream fileOutputStream = new FileOutputStream(file)) {
                bufferedWriter = new BufferedWriter(new OutputStreamWriter(fileOutputStream, StandardCharsets.UTF_8));

                for (TranslationPosition position : entry.getValue()) {
                    bufferedWriter.write(position.getKey() + " = " + position.getTargetValue());
                    bufferedWriter.newLine();
                }

                bufferedWriter.flush();
            } catch (IOException e) {
                throw new IllegalStateException(e.getMessage(), e);
            } finally {
                IOUtils.closeQuietly(bufferedWriter);
            }
        }
    }

    private static Map<String, List<TranslationPosition>> getTranslationPositions(final String project, final String fromLanguage,
            final String toLanguage) {
        File file = MissingTranslationsHelper.getTranslationsFile(project, MissingTranslationsHelper.L_XLSX);

        List<TranslationPosition> positions = Lists.newArrayList();

        try (FileInputStream fis = new FileInputStream(file)) {
            XSSFWorkbook workbook = new XSSFWorkbook(fis);
            XSSFSheet sheet = workbook.getSheetAt(0);

            for (int rowIndex = 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
                Row row = sheet.getRow(rowIndex);

                if (Objects.isNull(row)) {
                    break;
                }

                String path = formatCell(row.getCell(0, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL), toLanguage);
                String key = formatCell(row.getCell(1, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL), toLanguage);
                String value = formatCell(row.getCell(2, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL), toLanguage);
                String targetValue = formatCell(row.getCell(3, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL), toLanguage);

                TranslationPosition position = MissingTranslationsHelper.createTranslationPosition(path, key, value, targetValue);

                positions.add(position);
            }
        } catch (IOException e) {
            throw new IllegalStateException(e.getMessage(), e);
        }

        return positions.stream().collect(Collectors.groupingBy(TranslationPosition::getPath));
    }

    private static String formatCell(final Cell cell, final String fromLanguage) {
        Locale locale = new Locale(fromLanguage);

        final DataFormatter dataFormatter = new DataFormatter(Objects.isNull(locale) ? Locale.getDefault() : locale);

        if (Objects.nonNull(cell)) {
            CellType cellType = cell.getCellTypeEnum();

            if (cellType == CellType.FORMULA) {
                if (cell.getCachedFormulaResultTypeEnum() == CellType.ERROR) {
                    return "";
                } else {
                    return cell.getRichStringCellValue().getString().trim();
                }
            } else {
                return dataFormatter.formatCellValue(cell).trim();
            }
        } else {
            return "";
        }
    }

}
