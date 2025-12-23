package ru.malik.savefrom.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LinkParser {

    private static final String URL_REGEX = "(https?://\\S+)";

    public static String extractUrl(String message){

        Pattern pattern = Pattern.compile(URL_REGEX);
        Matcher matcher = pattern.matcher(message);

        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }

    public static String extractComment(String message, String url){
        if (url == null) return message;

        String comment = message.replace(url, "").trim();
        return comment.isEmpty() ? null : comment;
    }
}
