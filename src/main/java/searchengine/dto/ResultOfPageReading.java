package searchengine.dto;

import lombok.Data;

@Data
public class ResultOfPageReading {
    private String content;
    private int code;
    private String error;

    public ResultOfPageReading(String content, int code, String error) {
        this.content = content;
        this.code = code;
        this.error = error;
    }
}
