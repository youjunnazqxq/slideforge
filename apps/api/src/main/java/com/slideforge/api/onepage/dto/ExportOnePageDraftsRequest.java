package com.slideforge.api.onepage.dto;

import java.util.List;

public record ExportOnePageDraftsRequest(
        List<String> draftIds
) {
}
