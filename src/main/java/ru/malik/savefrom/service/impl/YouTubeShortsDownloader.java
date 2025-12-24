package ru.malik.savefrom.service.impl;


public class YouTubeShortsDownloader extends AbstractYtDlpDownloader {

    @Override
    public boolean canHandle(String url) {
        return url.contains("youtube.com") || url.contains("youtu.be");
    }
}
