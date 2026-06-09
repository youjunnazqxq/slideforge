package com.slideforge.api.research;

import java.util.List;

public interface SearchClient {

    boolean available();

    List<SearchResult> search(String query);
}
