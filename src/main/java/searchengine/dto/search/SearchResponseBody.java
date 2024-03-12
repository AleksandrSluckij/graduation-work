package searchengine.dto.search;

import lombok.Data;

import java.util.List;

@Data
public class SearchResponseBody {
    private boolean result;
    private int count;
    private List<SingleSearchResultDto> data;

    public SearchResponseBody (SearchResultDto incomeResult) {
        this.result = incomeResult.isResult();
        this.count = incomeResult.getCount();
        this.data = incomeResult.getData();
    }
}
