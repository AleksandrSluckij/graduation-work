package searchengine.services.search;

import searchengine.dto.search.SearchResultDto;

import java.util.Map;

public interface SearchService {
    SearchResultDto processQuery(Map<String, String> parameters);
}
