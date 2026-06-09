package com.slideforge.api.research;

import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

@Primary
@Component
public class TavilySearchClient implements SearchClient {

    private final RestClient restClient;
    private final String apiKey;

    public TavilySearchClient(@Value("${slideforge.search.tavily.api-key:}") String apiKey) {
        this.restClient = RestClient.builder()
                .baseUrl("https://api.tavily.com")
                .build();
        this.apiKey = apiKey;
    }

    @Override
    public boolean available() {
        return StringUtils.hasText(apiKey);
    }

    @Override
    public List<SearchResult> search(String query) {
        if (!available()) {
            return List.of();
        }

        TavilyResponse response = restClient.post()
                .uri("/search")
                .contentType(MediaType.APPLICATION_JSON)
                .body(new TavilyRequest(apiKey, query, "basic", 5))
                .retrieve()
                .body(TavilyResponse.class);

        if (response == null || response.results() == null) {
            return List.of();
        }

        return response.results().stream()
                .limit(5)
                .map(result -> new SearchResult(
                        stableSourceId(result.url()),
                        result.title(),
                        result.url(),
                        publisherFromUrl(result.url()),
                        "",
                        result.content()
                ))
                .toList();
    }

    private String stableSourceId(String url) {
        return "src-" + Math.abs((url == null ? "" : url).hashCode());
    }

    private String publisherFromUrl(String url) {
        if (!StringUtils.hasText(url)) {
            return "";
        }

        String normalized = url.replace("https://", "").replace("http://", "");
        int slashIndex = normalized.indexOf('/');
        return slashIndex < 0 ? normalized : normalized.substring(0, slashIndex);
    }

    private record TavilyRequest(
            String api_key,
            String query,
            String search_depth,
            Integer max_results
    ) {
    }

    private record TavilyResponse(
            List<TavilyResult> results
    ) {
    }

    private record TavilyResult(
            String title,
            String url,
            String content
    ) {
    }
}
