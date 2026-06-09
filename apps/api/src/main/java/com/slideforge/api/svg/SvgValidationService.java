package com.slideforge.api.svg;

import com.slideforge.api.onepage.dto.ValidationReport;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class SvgValidationService {

    public String sanitize(String rawSvg) {
        if (!StringUtils.hasText(rawSvg)) {
            return "";
        }

        String sanitized = rawSvg.trim();
        sanitized = sanitized.replaceAll("(?is)<script.*?</script>", "");
        sanitized = sanitized.replaceAll("(?is)<foreignObject.*?</foreignObject>", "");
        sanitized = sanitized.replaceAll("(?i)\\s+on[a-z]+\\s*=\\s*\"[^\"]*\"", "");
        sanitized = sanitized.replaceAll("(?i)\\s+on[a-z]+\\s*=\\s*'[^']*'", "");
        sanitized = sanitized.replaceAll("(?i)javascript:", "");

        return sanitized;
    }

    public ValidationReport validate(String svg) {
        List<String> warnings = new ArrayList<>();

        if (!StringUtils.hasText(svg)) {
            warnings.add("SVG 内容为空");
            return new ValidationReport(false, warnings);
        }

        String normalized = svg.toLowerCase(Locale.ROOT);

        if (!normalized.contains("<svg")) {
            warnings.add("缺少 svg 根节点");
        }

        if (!svg.contains("viewBox=\"0 0 1280 720\"") && !svg.contains("viewBox='0 0 1280 720'")) {
            warnings.add("viewBox 不是 0 0 1280 720");
        }

        if (normalized.contains("<script")) {
            warnings.add("包含 script 标签");
        }

        if (normalized.contains("foreignobject")) {
            warnings.add("包含 foreignObject 标签");
        }

        if (normalized.contains("http://") || normalized.contains("https://")) {
            warnings.add("包含外部资源 URL");
        }

        if (normalized.contains("javascript:")) {
            warnings.add("包含 javascript URL");
        }

        return new ValidationReport(warnings.isEmpty(), warnings);
    }
}
