package searchengine.dto;

import lombok.Data;

@Data
public class ErrorResponseBody {
    private final boolean result = false;
    private String error;

    public ErrorResponseBody (String error) {
        this.error = error;
    }
}
