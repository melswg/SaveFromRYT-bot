package ru.malik.savefrom.service;

import ru.malik.savefrom.model.MediaContent;
import ru.malik.savefrom.service.impl.InstagramDownloader;
import ru.malik.savefrom.service.impl.TikTokDownloader;
import ru.malik.savefrom.service.impl.YouTubeShortsDownloader;

import java.util.ArrayList;
import java.util.List;

public class DownloadManager {
    private final List<MediaDownloader> downloaders;

    public DownloadManager(){
        this.downloaders = new ArrayList<>();
        this.downloaders.add(new TikTokDownloader());
        this.downloaders.add(new YouTubeShortsDownloader());
        this.downloaders.add(new InstagramDownloader());
    }

    public MediaContent download(String url){
        for (MediaDownloader downloader : downloaders){
            if (downloader.canHandle(url)){
                return downloader.download(url);
            }
        }
        throw new IllegalArgumentException("Я не узнал эту ссылку: " + url);
    }
}
