package com.slideforge.api.research;

public record SearchResult(
        String id,
        String title,
        String url,
        String publisher,
        String publishedAt,
        String snippet
) {
}
