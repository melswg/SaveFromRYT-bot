package ru.malik.savefrom.service.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.malik.savefrom.model.MediaContent;
import ru.malik.savefrom.service.ContentTooLargeException;

import java.util.ArrayList;
import java.util.List;

public class YouTubeShortsDownloader extends AbstractYtDlpDownloader {
    private static final Logger log = LoggerFactory.getLogger(YouTubeShortsDownloader.class);

    private static final int DEFAULT_MAX_VIDEO_HEIGHT = 720;
    private static final int DEFAULT_MAX_VIDEO_DURATION_SECONDS = 600;
    private static final String DEFAULT_MAX_VIDEO_SIZE = "49M";
    private static final String DEFAULT_MAX_AUDIO_SIZE = "49M";

    @Override
    public boolean canHandle(String url) {
        return url.contains("youtube.com") || url.contains("youtu.be");
    }

    @Override
    public MediaContent download(String url) {
        RuntimeException videoFailure;

        try {
            return downloadWithExtraArgs(url, getVideoArgs(), "YTDLP");
        } catch (RuntimeException e) {
            videoFailure = e;
            log.warn("YouTube video download failed, trying audio fallback: {}", e.getMessage());
        }

        try {
            return downloadWithExtraArgs(url, getAudioArgs(), "YTDLP");
        } catch (RuntimeException audioFailure) {
            if (isLimitFailure(videoFailure) || isLimitFailure(audioFailure)) {
                throw new ContentTooLargeException("YouTube content is too large to send", audioFailure);
            }
            throw audioFailure;
        }
    }

    private List<String> getVideoArgs() {
        int maxHeight = getIntEnv("YOUTUBE_MAX_VIDEO_HEIGHT", DEFAULT_MAX_VIDEO_HEIGHT);
        int maxDuration = getIntEnv("YOUTUBE_MAX_VIDEO_DURATION_SECONDS", DEFAULT_MAX_VIDEO_DURATION_SECONDS);
        String maxSize = getStringEnv("YOUTUBE_MAX_VIDEO_SIZE", DEFAULT_MAX_VIDEO_SIZE);

        List<String> args = new ArrayList<>();
        args.add("-f");
        args.add(String.format(
                "bestvideo[height<=%d][ext=mp4][vcodec^=avc1]+bestaudio[ext=m4a]/best[height<=%d][ext=mp4]/best[height<=%d]",
                maxHeight, maxHeight, maxHeight
        ));
        args.add("--max-filesize");
        args.add(maxSize);
        args.add("--match-filter");
        args.add("duration <= " + maxDuration);
        return args;
    }

    private List<String> getAudioArgs() {
        String maxSize = getStringEnv("YOUTUBE_MAX_AUDIO_SIZE", DEFAULT_MAX_AUDIO_SIZE);

        return List.of(
                "-f", "bestaudio/best",
                "--max-filesize", maxSize,
                "-x",
                "--audio-format", "mp3",
                "--audio-quality", "0"
        );
    }

    private boolean isLimitFailure(RuntimeException exception) {
        if (exception == null || exception.getMessage() == null) {
            return false;
        }

        String message = exception.getMessage().toLowerCase();
        return message.contains("max-filesize")
                || message.contains("larger than")
                || message.contains("does not pass filter")
                || message.contains("duration <=");
    }

    private int getIntEnv(String name, int fallback) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) {
            return fallback;
        }

        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            log.warn("Некорректное значение {}={}, используется {}", name, value, fallback);
            return fallback;
        }
    }

    private String getStringEnv(String name, String fallback) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return value;
    }
}
