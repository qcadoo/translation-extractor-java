package com.qcadoo;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.apache.commons.io.FileUtils;

import com.google.common.collect.Lists;

public class ExtractMissingTranslations {

    public static void main(String[] args) throws IOException {
        String path = "/Users/krzysztofjelinski/china/mix";
        String fromLanguage = "pl";
        String toLanguage = "cn";
        File dir = new File(path);
        Collection<File> files = FileUtils.listFiles(dir, new String[] { "properties" }, true);
        for (File file : files) {
            String fileName = file.getName().toLowerCase();
            if (fileName.endsWith(fromLanguage + ".properties")) {
                Optional<File> targetFile = files.stream().filter(f -> f.getName().toLowerCase()
                        .equals(fileName.replace(fromLanguage + ".properties", toLanguage + ".properties"))).findAny();
                if (targetFile.isPresent()) {
                    BufferedReader source = new BufferedReader(new FileReader(file));
                    BufferedReader target = new BufferedReader(new FileReader(targetFile.get()));
                    List<String> cnKeys = Lists.newArrayList();
                    String line;
                    while ((line = target.readLine()) != null) {
                        int ind = line.indexOf("=");
                        if (ind > 0) {
                            cnKeys.add(line.substring(0, ind).trim());
                        }
                    }
                    boolean somethingMissing = false;
                    while ((line = source.readLine()) != null) {
                        int ind = line.indexOf("=");
                        if (ind > 0) {
                            String key = line.substring(0, ind).trim();
                            if (!cnKeys.contains(key)) {
                                Files.write(Paths.get(targetFile.get().getAbsolutePath()), (key + "=\n").getBytes(),
                                        StandardOpenOption.APPEND);
                                somethingMissing = true;
                            }
                        }
                    }
                    if (somethingMissing) {
                        System.out.println(targetFile.get().getPath().replace(path + "/", ""));
                    }

                }
            }
        }
    }
}
