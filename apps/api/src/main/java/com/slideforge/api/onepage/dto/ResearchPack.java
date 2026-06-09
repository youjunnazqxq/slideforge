package com.slideforge.api.onepage.dto;

import java.util.List;

public record ResearchPack(
        String mode,
        String summary,
        List<String> keyPoints,
        List<Evidence> evidence,
        List<Source> sources,
        List<String> limitations
) {

    public record Evidence(String claim, String support, List<String> sourceIds) {
    }

    public record Source(String id, String title, String url, String publisher, String publishedAt, String snippet) {
    }
}
