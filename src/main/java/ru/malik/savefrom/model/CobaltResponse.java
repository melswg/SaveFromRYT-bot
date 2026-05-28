package ru.malik.savefrom.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;
import java.util.Map;


@JsonIgnoreProperties(ignoreUnknown = true)
public class CobaltResponse {
    private String status;
    private String url;
    private String filename;
    private String type;
    private String service;
    private List<String> tunnel;
    private Output output;
    private Audio audio;
    private Boolean isHLS;
    private List<PickerItem> picker;

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }

    public String getFilename() { return filename; }
    public void setFilename(String filename) { this.filename = filename; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getService() { return service; }
    public void setService(String service) { this.service = service; }

    public List<String> getTunnel() { return tunnel; }
    public void setTunnel(List<String> tunnel) { this.tunnel = tunnel; }

    public Output getOutput() { return output; }
    public void setOutput(Output output) { this.output = output; }

    public Audio getAudio() { return audio; }
    public void setAudio(Audio audio) { this.audio = audio; }

    public Boolean getIsHLS() { return isHLS; }
    public void setIsHLS(Boolean isHLS) { this.isHLS = isHLS; }

    public List<PickerItem> getPicker() { return picker; }
    public void setPicker(List<PickerItem> picker) { this.picker = picker; }


    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class PickerItem {
        private String url;
        private String type; // "photo" or "video"

        public String getUrl() { return url; }
        public void setUrl(String url) { this.url = url; }
        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Output {
        private String type;
        private String filename;
        private Map<String, String> metadata;
        private Boolean subtitles;

        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        public String getFilename() { return filename; }
        public void setFilename(String filename) { this.filename = filename; }
        public Map<String, String> getMetadata() { return metadata; }
        public void setMetadata(Map<String, String> metadata) { this.metadata = metadata; }
        public Boolean getSubtitles() { return subtitles; }
        public void setSubtitles(Boolean subtitles) { this.subtitles = subtitles; }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Audio {
        private Boolean copy;
        private String format;
        private String bitrate;
        private Boolean cover;
        private Boolean cropCover;

        public Boolean getCopy() { return copy; }
        public void setCopy(Boolean copy) { this.copy = copy; }
        public String getFormat() { return format; }
        public void setFormat(String format) { this.format = format; }
        public String getBitrate() { return bitrate; }
        public void setBitrate(String bitrate) { this.bitrate = bitrate; }
        public Boolean getCover() { return cover; }
        public void setCover(Boolean cover) { this.cover = cover; }
        public Boolean getCropCover() { return cropCover; }
        public void setCropCover(Boolean cropCover) { this.cropCover = cropCover; }
    }
}
