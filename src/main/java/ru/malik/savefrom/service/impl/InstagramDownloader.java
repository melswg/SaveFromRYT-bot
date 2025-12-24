package ru.malik.savefrom.service.impl;

import ru.malik.savefrom.model.MediaContent;
import ru.malik.savefrom.service.MediaDownloader;

import java.util.List;

public class InstagramDownloader extends AbstractYtDlpDownloader {

    @Override
    public boolean canHandle(String url) {
        return url.contains("instagram.com");
    }

    @Override
    protected List<String> getExtraArgs(){
        return List.of(
                "--user-agent",
                "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
        );
    }

}
