package service.impl;

import ru.malik.savefrom.model.MediaContent;
import service.MediaDownloader;

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
