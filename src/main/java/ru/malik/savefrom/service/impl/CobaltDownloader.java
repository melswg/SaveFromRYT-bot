package ru.malik.savefrom.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.malik.savefrom.model.CobaltResponse;
import ru.malik.savefrom.model.MediaContent;
import ru.malik.savefrom.service.MediaDownloader;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class CobaltDownloader implements MediaDownloader {

    private static final Logger log = LoggerFactory.getLogger(CobaltDownloader.class);
    private static final String DOWNLOAD_DIR = "downloads";// Или /var/lib/telegram-bot-api для Docker

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final String cobaltApiUrl;

    public CobaltDownloader() {
        this.httpClient = HttpClient.newHttpClient();
        this.objectMapper = new ObjectMapper();
        String envUrl = System.getenv("COBALT_API_URL");
        this.cobaltApiUrl = (envUrl != null ? envUrl : "http://localhost:9000");
    }

    @Override
    public boolean canHandle(String url) {
        return url.contains("tiktok.com") ||
                url.contains("instagram.com") ||
                url.contains("pinterest") ||
                url.contains("pin.it") ||
                url.contains("vk.ru") ||
                url.contains("vk.com") ||
                url.contains("twitter.com") ||
                url.contains("x.com") ||
                url.contains("soundcloud.com");
    }


    @Override
    public MediaContent download(String url) {
        try {
            // формировка запрос к кобальту
            RequestBody body = new RequestBody(url);
            String jsonBody = objectMapper.writeValueAsString(body);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(cobaltApiUrl))
                    .header("Content-Type", "application/json")
                    .header("Accept", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .build();

            // ответ от кобальта
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());


            if (response.statusCode() != 200) {
                log.error("Cobalt error! Status: {}. Body: {}", response.statusCode(), response.body());
                throw new RuntimeException("Cobalt server returned error code " + response.statusCode());
            }

            CobaltResponse cobaltData = objectMapper.readValue(response.body(), CobaltResponse.class);

            String status = cobaltData.getStatus();
            if (!"stream".equals(status) && !"picker".equals(status) && !"redirect".equals(status) && !"tunnel".equals(status)) {
                throw new RuntimeException("Cobalt returned unexpected status: " + status);
            }

            // подготовка папки
            String requestId = UUID.randomUUID().toString();
            Path requestDir = Paths.get(DOWNLOAD_DIR, requestId);
            Files.createDirectories(requestDir);

            List<File> downloadedFiles = new ArrayList<>();
            boolean isVideo = false;

            // установка файлов
            if (cobaltData.getPicker() != null) {
                int index = 1;
                for (CobaltResponse.PickerItem item : cobaltData.getPicker()) {
                    log.info("Cobalt item type: {}, URL: {}", item.getType(), item.getUrl());
                    File file = downloadFile(item.getUrl(), requestDir, index++, item.getType());
                    downloadedFiles.add(file);

                    if ("video".equals(item.getType())) isVideo = true;
                }
            } else {
                String type = null;

                if (cobaltData.getFilename() != null) {
                    String fname = cobaltData.getFilename().toLowerCase();
                    if (fname.endsWith(".jpg") || fname.endsWith(".jpeg") || fname.endsWith(".png") || fname.endsWith(".webp")) {
                        type = "photo";
                    } else if (fname.endsWith(".gif")) {
                        type = "gif";
                    } else if (fname.endsWith(".mp3") || fname.endsWith(".wav")) {
                        type = "audio";
                    }
                }

                log.info("Single file. Filename: {}, Type detected: {}", cobaltData.getFilename(), type);

                File file = downloadFile(cobaltData.getUrl(), requestDir, 1, type);
                downloadedFiles.add(file);

                isVideo = file.getName().endsWith(".mp4");
            }

            return new MediaContent(downloadedFiles, isVideo, "COBALT");

        } catch (Exception e) {
            log.error("Ошибка при работе с Cobalt: ", e);
            throw new RuntimeException("Cobalt download failed", e);
        }
    }



    private File downloadFile(String fileUrl, Path dir, int index, String type) throws IOException {
        String ext = ".mp4";

        if (type != null) {
            if ("photo".equals(type) || "image".equals(type)) {
                ext = ".jpg";
            } else if ("gif".equals(type)) {
                ext = ".gif";
            } else if ("audio".equals(type)) {
                ext = ".mp3";
            }
        } else {
            String lowerUrl = fileUrl.toLowerCase();
            if (lowerUrl.contains(".jpg") || lowerUrl.contains(".jpeg") || lowerUrl.contains(".png") || lowerUrl.contains(".webp")) {
                ext = ".jpg";
            } else if (lowerUrl.contains(".gif")) {
                ext = ".gif";
            }
        }

        String fileName = String.format("%05d%s", index, ext);
        Path targetPath = dir.resolve(fileName);

        try (InputStream in = new URL(fileUrl).openStream()) {
            Files.copy(in, targetPath, StandardCopyOption.REPLACE_EXISTING);
        }
        return targetPath.toFile();
    }

    private static class RequestBody {
        public String url;
        public RequestBody(String url) { this.url = url; }
    }
}