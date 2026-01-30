package ru.malik.savefrom.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;

public class VideoInfoExtractor {
    private static final Logger log = LoggerFactory.getLogger(VideoInfoExtractor.class);

    public record VideoMetadata(int width, int height, int duration) {}

    public static VideoMetadata getMetadata(File videoFile) {
        try {
            ProcessBuilder pb = new ProcessBuilder(
                    "ffprobe",
                    "-v", "error",
                    "-select_streams", "v:0",
                    "-show_entries", "stream=width,height,duration",
                    "-of", "default=noprint_wrappers=1:nokey=1",
                    videoFile.getAbsolutePath()
            );

            Process process = pb.start();

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String widthStr = reader.readLine();
                String heightStr = reader.readLine();
                String durationStr = reader.readLine();

                int width = (widthStr != null) ? Integer.parseInt(widthStr.trim()) : 0;
                int height = (heightStr != null) ? Integer.parseInt(heightStr.trim()) : 0;

                int duration = 0;
                if (durationStr != null && !durationStr.equals("N/A")) {
                    double d = Double.parseDouble(durationStr.trim());
                    duration = (int) Math.round(d);
                }
                return new VideoMetadata(width, height, duration);

            }
        } catch (Exception e) {
            log.error("Не удалось извлечь метаданные для {}: {}", videoFile.getName(), e.getMessage());
            return null;
        }
    }

    public static File extractThumbnail(File videoFile) {
        try {
            File thumbFile = new File(videoFile.getParent(), "thumb_" + System.currentTimeMillis() + ".jpg");

            ProcessBuilder pb = new ProcessBuilder(
                    "ffmpeg",
                    "-y",
                    "-i", videoFile.getAbsolutePath(),
                    "-ss", "00:00:01",
                    "-vframes", "1",
                    "-vf", "scale=320:-1",
                    "-q:v", "2",
                    thumbFile.getAbsolutePath()
            );

            Process p = pb.start();
            int exitCode = p.waitFor();

            if (exitCode == 0 && thumbFile.exists() && thumbFile.length() > 0) {
                return thumbFile;
            }
        } catch (Exception e) {
            log.error("Не удалось создать превью: ", e);
        }
        return null;
    }
}
