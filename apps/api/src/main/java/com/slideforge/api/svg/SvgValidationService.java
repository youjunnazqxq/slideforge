package com.slideforge.api.svg;

import com.slideforge.api.onepage.dto.ValidationReport;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class SvgValidationService {

    private static final int CANVAS_WIDTH = 1280;
    private static final int CANVAS_HEIGHT = 720;
    private static final Pattern RECT_PATTERN = Pattern.compile("<rect\\b[^>]*>", Pattern.CASE_INSENSITIVE);
    private static final Pattern TEXT_PATTERN = Pattern.compile("<text\\b([^>]*)>(.*?)</text>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
    private static final Pattern ATTRIBUTE_PATTERN = Pattern.compile("\\b([a-zA-Z_:][-a-zA-Z0-9_:.]*)\\s*=\\s*['\"]([^'\"]*)['\"]");

    public String sanitize(String rawSvg) {
        if (!StringUtils.hasText(rawSvg)) {
            return "";
        }

        String sanitized = rawSvg.trim();
        sanitized = sanitized.replaceAll("(?is)<script.*?</script>", "");
        sanitized = sanitized.replaceAll("(?is)<foreignObject.*?</foreignObject>", "");
        sanitized = sanitized.replaceAll("(?is)<image\\b[^>]*(https?://|data:)[^>]*/?>", "");
        sanitized = sanitized.replaceAll("(?i)\\s+on[a-z]+\\s*=\\s*\"[^\"]*\"", "");
        sanitized = sanitized.replaceAll("(?i)\\s+on[a-z]+\\s*=\\s*'[^']*'", "");
        sanitized = sanitized.replaceAll("(?i)javascript:", "");
        sanitized = sanitized.replaceAll("(?is)url\\s*\\([^)]*\\)", "");

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

        validateRectBounds(svg, warnings);
        validateTextBoundsAndDensity(svg, warnings);
        validateElementDensity(svg, warnings);

        return new ValidationReport(warnings.isEmpty(), warnings);
    }

    private void validateRectBounds(String svg, List<String> warnings) {
        Matcher matcher = RECT_PATTERN.matcher(svg);
        int index = 0;

        while (matcher.find()) {
            index++;
            String rect = matcher.group();
            double x = attributeNumber(rect, "x", 0);
            double y = attributeNumber(rect, "y", 0);
            double width = attributeNumber(rect, "width", 0);
            double height = attributeNumber(rect, "height", 0);

            if (x < 0 || y < 0 || width < 0 || height < 0) {
                warnings.add("第 " + index + " 个 rect 含负数坐标或尺寸");
            }

            if (x + width > CANVAS_WIDTH || y + height > CANVAS_HEIGHT) {
                warnings.add("第 " + index + " 个 rect 超出 1280x720 画布");
            }
        }
    }

    private void validateTextBoundsAndDensity(String svg, List<String> warnings) {
        Matcher matcher = TEXT_PATTERN.matcher(svg);
        int index = 0;
        int longTextCount = 0;

        while (matcher.find()) {
            index++;
            String attributes = matcher.group(1);
            String content = matcher.group(2).replaceAll("<[^>]+>", "").trim();
            double x = attributeNumber(attributes, "x", 0);
            double y = attributeNumber(attributes, "y", 0);

            if (x < 0 || y < 0 || x > CANVAS_WIDTH || y > CANVAS_HEIGHT) {
                warnings.add("第 " + index + " 个 text 坐标超出画布");
            }

            if (content.length() > 72) {
                longTextCount++;
            }
        }

        if (index == 0) {
            warnings.add("SVG 中没有可见 text 元素");
        }

        if (index > 36) {
            warnings.add("text 元素过多，可能导致页面信息密度过高");
        }

        if (longTextCount > 0) {
            warnings.add("存在 " + longTextCount + " 段过长文本，建议拆成短句或换行");
        }
    }

    private void validateElementDensity(String svg, List<String> warnings) {
        int rectCount = countMatches(svg, RECT_PATTERN);

        if (rectCount > 48) {
            warnings.add("rect 元素过多，Bento Grid 可能过碎");
        }
    }

    private int countMatches(String value, Pattern pattern) {
        Matcher matcher = pattern.matcher(value);
        int count = 0;

        while (matcher.find()) {
            count++;
        }

        return count;
    }

    private double attributeNumber(String value, String attributeName, double fallback) {
        Matcher matcher = ATTRIBUTE_PATTERN.matcher(value);

        while (matcher.find()) {
            if (attributeName.equalsIgnoreCase(matcher.group(1))) {
                try {
                    return Double.parseDouble(matcher.group(2));
                } catch (NumberFormatException exception) {
                    return fallback;
                }
            }
        }

        return fallback;
    }
}
