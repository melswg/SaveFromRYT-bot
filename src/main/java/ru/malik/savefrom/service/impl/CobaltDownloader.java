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
        return url.contains("tiktok.com") || url.contains("instagram.com");
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
                    File file = downloadFile(item.getUrl(), requestDir, index++, item.getType());
                    downloadedFiles.add(file);
                    if ("video".equals(item.getType())) isVideo = true;
                }
            } else {
                String type = "video";
                if (url.contains("/photo/") || url.contains("/p/")) type = "photo";

                File file = downloadFile(cobaltData.getUrl(), requestDir, 1, null);
                downloadedFiles.add(file);
                isVideo = file.getName().endsWith(".mp4");
            }

            return new MediaContent(downloadedFiles, isVideo);

        } catch (Exception e) {
            log.error("Ошибка при работе с Cobalt: ", e);
            throw new RuntimeException("Cobalt download failed", e);
        }
    }



    private File downloadFile(String fileUrl, Path dir, int index, String type) throws IOException {
        String ext = ".mp4";

        if (type != null) {
            if ("photo".equals(type) || "image".equals(type)) ext = ".jpg";
        } else {
            if (fileUrl.contains(".jpg") || fileUrl.contains(".webp") || fileUrl.contains(".png")) {
                ext = ".jpg";
            }
        }

        String fileName = String.format("%05d%s", index, ext);
        Path targetPath = dir.resolve(fileName);

        try (InputStream in = new URL(fileUrl).openStream()) {
            Files.copy(in, targetPath, StandardCopyOption.REPLACE_EXISTING);
        }
        return targetPath.toFile();
    }

    // помощник класс для отправки JSON
    private static class RequestBody {
        public String url;
        public String videoQuality = "max";

        public RequestBody(String url) { this.url = url; }
    }
}