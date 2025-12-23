package ru.malik.savefrom.service;

import ru.malik.savefrom.model.MediaContent;

public interface MediaDownloader {

    MediaContent download(String url);
    boolean canHandle(String url);
}
