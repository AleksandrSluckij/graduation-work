package searchengine.dto.search;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
public class SearchResultDto {
    private boolean result;
    private String error;
    private int count;
    private List<SingleSearchResultDto> data;

    public SearchResultDto (String error) {
        this.result = false;
        this.error = error;
    }

    public SearchResultDto (int count, List<SingleSearchResultDto> data) {
        this.result = true;
        this.count = count;
        this.data = data;
    }
}
