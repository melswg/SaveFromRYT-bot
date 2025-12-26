package ru.malik.savefrom.service.impl;

public class RuTubeDownloader extends AbstractYtDlpDownloader{

    @Override
    public boolean canHandle(String url) {
        return url.contains("rutube.ru");
    }
}
