package ru.malik.savefrom.service.impl;

public class TwitchDownloader extends AbstractYtDlpDownloader{

    @Override
    public boolean canHandle(String url) {
        return url.contains("twitch.tv");
    }

}
