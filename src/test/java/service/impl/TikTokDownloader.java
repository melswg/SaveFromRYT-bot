package service.impl;

import ru.malik.savefrom.model.MediaContent;
import service.MediaDownloader;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;

public class TikTokDownloader implements MediaDownloader {
    private static final String DOWNLOAD_DIR = "downloads";


    @Override
    public boolean canHandle(String url) {
        return url.contains("tiktok.com");
    }

    @Override
    public MediaContent download(String url) {
        try {
            String requestID =UUID.randomUUID().toString();
            Path requestDir = Paths.get(DOWNLOAD_DIR, requestID);
            Files.createDirectories(requestDir);

            String outputTemplate = requestDir.resolve("%(autonumber)s.%(ext)s").toString(); // нумеровка !!!

            ProcessBuilder pb = new ProcessBuilder(
                    "yt-dlp",
                    "--impersonate", "chrome",
                    "--no-playlist",
                    "-o", outputTemplate,
                    url
            );

            pb.inheritIO();
            Process process = pb.start();
            int exitCode = process.waitFor();

            if (exitCode == 0){ //  && Files.exists(fullPath) в условия
                List<File> downloadFiles = Files.list(requestDir)
                        .map(Path::toFile)
                        .sorted()
                        .toList();
                if (downloadFiles.isEmpty()){
                    throw new RuntimeException("No files found after download");
                }

                boolean isVideo = downloadFiles.get(0).getName().endsWith(".mp4")
                        || downloadFiles.get(0).getName().endsWith(".webm");

                return new MediaContent(downloadFiles, isVideo);
            } else {
                throw new RuntimeException("yt-dlp failed with exit code: " + exitCode);
            }

        }catch (IOException | InterruptedException e){
            e.printStackTrace();
            return null;
        }
    }

}
