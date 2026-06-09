package com.slideforge.api.research;

import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class NoopSearchClient implements SearchClient {

    @Override
    public boolean available() {
        return false;
    }

    @Override
    public List<SearchResult> search(String query) {
        return List.of();
    }
}
