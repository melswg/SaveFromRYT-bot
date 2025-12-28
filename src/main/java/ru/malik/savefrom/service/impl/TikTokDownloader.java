package ru.malik.savefrom.service.impl;


import java.util.List;

public class TikTokDownloader extends AbstractYtDlpDownloader {

    @Override
    public boolean canHandle(String url) {
        return url.contains("tiktok.com");
    }

    @Override
    protected List<String> getExtraArgs() {
        return List.of(
                "--user-agent",
                "Mozilla/5.0 (iPhone; CPU iPhone OS 16_6 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/16.6 Mobile/15E148 Safari/604.1"
        );
    }
}
