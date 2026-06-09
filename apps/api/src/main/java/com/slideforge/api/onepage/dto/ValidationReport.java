package com.slideforge.api.onepage.dto;

import java.util.List;

public record ValidationReport(
        boolean valid,
        List<String> warnings
) {
}
