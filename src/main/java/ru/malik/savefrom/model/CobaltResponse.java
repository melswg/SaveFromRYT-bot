package ru.malik.savefrom.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;


@JsonIgnoreProperties(ignoreUnknown = true)
public class CobaltResponse {
    private String status;
    private String url;
    private String filename;
    private List<PickerItem> picker;

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }

    public String getFilename() { return filename; }
    public void setFilename(String filename) { this.filename = filename; }

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
}
