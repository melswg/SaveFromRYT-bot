package ru.malik.savefrom.service.impl;

import ru.malik.savefrom.model.MediaContent;
import ru.malik.savefrom.service.MediaDownloader;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public abstract class AbstractYtDlpDownloader implements MediaDownloader {
    private static final String DOWNLOAD_DIR = "downloads";
    private static final String DEFAULT_COOKIES_PATH = "/app/yt-dlp-cookies.txt";
    private static final int MAX_PROCESS_OUTPUT_CHARS = 8000;

    public MediaContent download(String url){
        return downloadWithExtraArgs(url, getExtraArgs(), "YTDLP");
    }

    protected MediaContent downloadWithExtraArgs(String url, List<String> extraArgs, String source){
        try {
            String requestID = UUID.randomUUID().toString();
            Path requestDir = Paths.get(DOWNLOAD_DIR, requestID);
            Files.createDirectories(requestDir);

            String outputTemplate = requestDir.resolve("%(autonumber)s.%(ext)s").toString(); // нумеровка !!!

            List<String> command = new ArrayList<>();
            command.add("yt-dlp");

            command.add("--force-ipv4");

            command.add("--no-playlist");

            String cookiesPath = getCookiesPath();
            File cookies = new File(cookiesPath);
            if (cookies.exists()) {
                command.add("--cookies");
                command.add(cookiesPath);
            }

            command.add("--rm-cache-dir");

            command.add("-N");
            command.add("5");
            command.add("-f");
            command.add("bestvideo[vcodec^=avc]+bestaudio[ext=m4a]/best[ext=mp4]/best");

            command.add("--socket-timeout");
            command.add("30");

            command.add("--no-check-certificate");
            command.add("-o");
            command.add(outputTemplate);

            command.addAll(extraArgs);
            command.add(url);

            ProcessBuilder pb = new ProcessBuilder(command);
            pb.redirectErrorStream(true);

            Process process = pb.start();
            StringBuilder processOutput = new StringBuilder();
            Thread outputReader = new Thread(() -> readProcessOutput(process, processOutput));
            outputReader.setDaemon(true);
            outputReader.start();

            int exitCode = process.waitFor();
            outputReader.join(1000);

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

                return new MediaContent(downloadFiles, isVideo, source);
            } else {
                throw new RuntimeException("yt-dlp failed with exit code: " + exitCode
                        + ". Output: " + processOutput);
            }

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Download interrupted", e);
        } catch (IOException e) {
            throw new RuntimeException("Download failed", e);
        }
    }

    private boolean isSupportedFile(String name) {
        String lower = name.toLowerCase();
        return lower.endsWith(".mp4") || lower.endsWith(".webm") || // Видео
                lower.endsWith(".jpg") || lower.endsWith(".jpeg") || lower.endsWith(".png") || lower.endsWith(".webp") ||
                lower.endsWith(".mp3") || lower.endsWith(".m4a") || lower.endsWith(".opus") || lower.endsWith(".wav");
    }

    protected List<String> getExtraArgs(){
        return new ArrayList<>();
    }

    private String getCookiesPath() {
        String envPath = System.getenv("YT_DLP_COOKIES_PATH");
        if (envPath == null || envPath.isBlank()) {
            return DEFAULT_COOKIES_PATH;
        }
        return envPath;
    }

    private void readProcessOutput(Process process, StringBuilder processOutput) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                process.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (processOutput.length() < MAX_PROCESS_OUTPUT_CHARS) {
                    processOutput.append(line).append(System.lineSeparator());
                }
            }
        } catch (IOException ignored) {
        }
    }
}
