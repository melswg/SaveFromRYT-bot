package ru.malik.savefrom.service.impl;


public class TikTokDownloader extends AbstractYtDlpDownloader {

    @Override
    public boolean canHandle(String url) {
        return url.contains("tiktok.com");
    }

}
