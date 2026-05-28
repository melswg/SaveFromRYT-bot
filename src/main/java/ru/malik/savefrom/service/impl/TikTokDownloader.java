package ru.malik.savefrom.service.impl;


import java.util.ArrayList;
import java.util.List;

public class TikTokDownloader extends AbstractYtDlpDownloader {

    @Override
    public boolean canHandle(String url) {
        return url.contains("tiktok.com");
    }

    @Override
    protected List<String> getExtraArgs() {
        List<String> args = new ArrayList<>();

        args.add("--user-agent");
        args.add("Mozilla/5.0 (iPhone; CPU iPhone OS 17_2 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.2 Mobile/15E148 Safari/604.1");

        args.add("--extractor-args");
        args.add("tiktok:api_hostname=api16-normal-c-useast1a.tiktokv.com;app_name=musical_ly;version_code=300904");

        args.add("--referer");
        args.add("https://www.tiktok.com/");

        args.add("--add-header");
        args.add("Sec-Fetch-Mode: navigate");

        return args;
    }
}
