package com.slideforge.api.svg;

import static org.assertj.core.api.Assertions.assertThat;

import com.slideforge.api.onepage.dto.ValidationReport;
import org.junit.jupiter.api.Test;

class SvgValidationServiceTest {

    private final SvgValidationService service = new SvgValidationService();

    @Test
    void validatesCleanPptxReadySvg() {
        String svg = """
                <svg width="1280" height="720" viewBox="0 0 1280 720" xmlns="http://www.w3.org/2000/svg">
                  <rect x="0" y="0" width="1280" height="720" fill="#ffffff"/>
                  <text x="80" y="120">SlideForge</text>
                </svg>
                """;

        ValidationReport report = service.validate(svg);

        assertThat(report.valid()).isTrue();
        assertThat(report.warnings()).isEmpty();
    }

    @Test
    void warnsForSvgThatIsRiskyForPptxImport() {
        String svg = """
                <svg width="1024" height="768" viewBox="0 0 1280 720">
                  <style>@import url('https://example.com/font.css');</style>
                  <filter id="blur"><feGaussianBlur stdDeviation="4"/></filter>
                  <image href="data:image/png;base64,AAAA"/>
                  <text x="80" y="120">SlideForge</text>
                """;

        ValidationReport report = service.validate(svg);

        assertThat(report.valid()).isFalse();
        assertThat(report.warnings()).contains(
                "SVG root is not closed",
                "SVG width should be 1280",
                "SVG height should be 720",
                "Inline style blocks are risky for PPTX import",
                "External font CSS is not allowed",
                "Embedded data URLs are not allowed",
                "SVG filters may not survive PPTX import"
        );
    }
}
