package ru.malik.savefrom.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.malik.savefrom.model.MediaContent;
import ru.malik.savefrom.service.impl.*;

import java.util.ArrayList;
import java.util.List;

public class DownloadManager {
    private static final Logger log = LoggerFactory.getLogger(DownloadManager.class);
    private final List<MediaDownloader> downloaders;

    private static final int MAX_RETRIES = 3;

    public DownloadManager() {
        this.downloaders = new ArrayList<>();

        this.downloaders.add(new CobaltDownloader());
        this.downloaders.add(new YouTubeShortsDownloader());
        this.downloaders.add(new TikTokDownloader());
        this.downloaders.add(new TwitchDownloader());
        this.downloaders.add(new RuTubeDownloader());
    }

    public MediaContent download(String url) {
        int attempt = 1;

        while (attempt <= MAX_RETRIES) {
            log.info("Попытка скачивания {} из {}", attempt, MAX_RETRIES);

            for (MediaDownloader downloader : downloaders) {
                if (downloader.canHandle(url)) {
                    try {
                        return downloader.download(url);
                    } catch (Exception e) {
                        log.warn("Загрузчик {} не справился: {}. Пробуем следующий...",
                                downloader.getClass().getSimpleName(), e.getMessage());
                    }
                }
            }

            attempt++;
            if (attempt <= MAX_RETRIES) {
                try {
                    long sleepTime = 2000L * attempt;
                    log.info("Пауза {} мс перед следующей попыткой...", sleepTime);
                    Thread.sleep(sleepTime);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
        throw new RuntimeException("Не удалось скачать контент после " + MAX_RETRIES + " попыток: " + url);
    }
}