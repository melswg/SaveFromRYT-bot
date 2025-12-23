package ru.malik.savefrom.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.stream.Stream;

public class FileCleaner {
    private static final Logger log = LoggerFactory.getLogger(FileCleaner.class);

    public static void cleanup(File directory){

        try (Stream<Path> walk = Files.walk(directory.toPath())){
            walk.sorted(Comparator.reverseOrder())
                    .map(Path::toFile)
                    .forEach(File::delete);
            log.info("Временные данные удалены: {}", directory.getName());
        } catch (Exception e){
            log.error("Не удалось очистить файлы {}: {}", directory.getName(), e.getMessage());
        }
    }

}
