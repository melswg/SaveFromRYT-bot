package ru.malik.savefrom.model;
//Сохранение результата (список путей к файлу, тип контента и т.д.)

import java.io.File;
import java.util.List;

public class MediaContent {

    private List<File> files;
    private String caption;
    private boolean isVideo;

    public MediaContent(List<File> files, String caption, boolean isVideo){
        this.files = files;
        this.caption = caption;
        this.isVideo = isVideo;
    }

    public MediaContent(List<File> files, boolean isVideo){
        this(files, null, isVideo);
    }

    public List<File> getFiles(){ return files; }
    public String getCaption(){ return caption; }
    public boolean isVideo() { return isVideo; }
}
