package ru.malik.savefrom.service.impl;

import ru.malik.savefrom.model.MediaContent;
import ru.malik.savefrom.service.MediaDownloader;

public class InstagramDownloader implements MediaDownloader {
    @Override
    public MediaContent download(String url) {
        return null;
    }

    @Override
    public boolean canHandle(String url) {
        return false;
    }
}
