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

    public static File fastFixVideo(File input) {
        String name = input.getName().toLowerCase();
        if (!name.endsWith(".mp4") && !name.endsWith(".webm") && !name.endsWith(".mkv")) {
            return input;
        }

        try {
            File output = new File(input.getParent(), "fixed_" + System.currentTimeMillis() + ".mp4");

            ProcessBuilder pb = new ProcessBuilder(
                    "ffmpeg", "-y", "-i", input.getAbsolutePath(),
                    "-c", "copy",
                    "-movflags", "+faststart",
                    "-strict", "unofficial",
                    output.getAbsolutePath()
            );

            Process p = pb.start();
            int exitCode = p.waitFor();

            if (exitCode == 0 && output.exists() && output.length() > 0) {
                log.info("Видео исправлено (fast fix): {}", output.getName());
                return output;
            } else {
                log.warn("Fast fix не удался, отправляем оригинал.");
                return input;
            }
        } catch (Exception e) {
            log.error("Ошибка при fast fix: ", e);
            return input;
        }
    }

}
