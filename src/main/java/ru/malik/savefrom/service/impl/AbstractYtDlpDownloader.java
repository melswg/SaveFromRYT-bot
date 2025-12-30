package ru.malik.savefrom.service.impl;

import ru.malik.savefrom.model.MediaContent;
import ru.malik.savefrom.service.MediaDownloader;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public abstract class AbstractYtDlpDownloader implements MediaDownloader {
    private static final String DOWNLOAD_DIR = "downloads";

    public MediaContent download(String url){
        try {
            String requestID = UUID.randomUUID().toString();
            Path requestDir = Paths.get(DOWNLOAD_DIR, requestID);
            Files.createDirectories(requestDir);

            String outputTemplate = requestDir.resolve("%(autonumber)s.%(ext)s").toString(); // нумеровка !!!

            List<String> command = new ArrayList<>();
            command.add("yt-dlp");
            command.add("--no-playlist");
            command.add("-f");
            command.add("bestvideo[vcodec^=avc]+bestaudio[acodec^=mp4a]/best[ext=mp4]/best");

            command.add("--merge-output-format");
            command.add("mp4");

            command.add("--no-check-certificate");

            command.add("-o");
            command.add(outputTemplate);

            command.addAll(getExtraArgs());
            command.add(url);

            ProcessBuilder pb = new ProcessBuilder(command);

            pb.inheritIO();
            Process process = pb.start();
            int exitCode = process.waitFor();

            if (exitCode == 0){ //  && Files.exists(fullPath) в условия
                List<File> downloadFiles = Files.list(requestDir)
                        .map(Path::toFile)
                        .filter(f -> isSupportedFile(f.getName()))
                        .sorted()
                        .toList();
                if (downloadFiles.isEmpty()){
                    throw new RuntimeException("No files found after download");
                }

                boolean isVideo = downloadFiles.stream()
                        .anyMatch(f -> f.getName().endsWith(".mp4") || f.getName().endsWith(".webm"));

                return new MediaContent(downloadFiles, isVideo);
            } else {
                throw new RuntimeException("yt-dlp failed with exit code: " + exitCode);
            }

        }catch (IOException | InterruptedException e){
            throw new RuntimeException("Download failed", e);
        }
    }

    private boolean isSupportedFile(String name) {
        String lower = name.toLowerCase();
        return lower.endsWith(".mp4") || lower.endsWith(".webm") || // Видео
                lower.endsWith(".jpg") || lower.endsWith(".jpeg") || lower.endsWith(".png") || lower.endsWith(".webp");
    }

    protected List<String> getExtraArgs(){
        return new ArrayList<>();
    }
}
