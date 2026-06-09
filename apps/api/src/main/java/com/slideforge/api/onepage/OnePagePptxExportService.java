package com.slideforge.api.onepage;

import java.awt.Dimension;
import java.awt.Rectangle;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.UUID;
import org.apache.poi.sl.usermodel.PictureData;
import org.apache.poi.xslf.usermodel.XMLSlideShow;
import org.apache.poi.xslf.usermodel.XSLFPictureData;
import org.apache.poi.xslf.usermodel.XSLFPictureShape;
import org.apache.poi.xslf.usermodel.XSLFSlide;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

@Service
public class OnePagePptxExportService {

    private static final int SLIDE_WIDTH = 1280;
    private static final int SLIDE_HEIGHT = 720;

    private final OnePageDraftRepository onePageDraftRepository;

    public OnePagePptxExportService(OnePageDraftRepository onePageDraftRepository) {
        this.onePageDraftRepository = onePageDraftRepository;
    }

    public ExportedPptx exportDraft(String draftId) {
        OnePageDraftEntity draft = onePageDraftRepository.findById(UUID.fromString(draftId))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "草稿不存在。"));

        if (!StringUtils.hasText(draft.getSvgContent())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "请先生成 SVG 后再导出 PPTX。");
        }

        assertExportReady(draft);

        try (XMLSlideShow ppt = new XMLSlideShow(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            ppt.setPageSize(new Dimension(SLIDE_WIDTH, SLIDE_HEIGHT));
            addSvgSlide(ppt, draft.getSvgContent());
            ppt.write(output);

            return new ExportedPptx(fileName(draft), output.toByteArray());
        } catch (IOException exception) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "PPTX 导出失败。");
        }
    }

    public ExportedPptx exportDrafts(List<String> draftIds) {
        if (draftIds == null || draftIds.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "请先选择要导出的页面草稿。");
        }

        List<OnePageDraftEntity> drafts = draftIds.stream()
                .map(draftId -> onePageDraftRepository.findById(toUuid(draftId))
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "草稿不存在。")))
                .toList();

        drafts.forEach(draft -> {
            if (!StringUtils.hasText(draft.getSvgContent())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "请先为所有页面生成 SVG 后再导出 PPTX。");
            }
        });

        drafts.forEach(this::assertExportReady);

        try (XMLSlideShow ppt = new XMLSlideShow(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            ppt.setPageSize(new Dimension(SLIDE_WIDTH, SLIDE_HEIGHT));

            for (OnePageDraftEntity draft : drafts) {
                addSvgSlide(ppt, draft.getSvgContent());
            }

            ppt.write(output);
            return new ExportedPptx("slideforge-deck-" + drafts.size() + "-pages.pptx", output.toByteArray());
        } catch (IOException exception) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "PPTX 导出失败。");
        }
    }

    private void assertExportReady(OnePageDraftEntity draft) {
        if (!"SVG_READY".equals(draft.getStatus())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "SVG is not validation-ready. Fix it before exporting PPTX.");
        }
    }

    private void addSvgSlide(XMLSlideShow ppt, String svgContent) {
        XSLFSlide slide = ppt.createSlide();
        XSLFPictureData pictureData = ppt.addPicture(
                svgContent.getBytes(),
                PictureData.PictureType.SVG
        );
        XSLFPictureShape picture = slide.createPicture(pictureData);
        picture.setAnchor(new Rectangle(0, 0, SLIDE_WIDTH, SLIDE_HEIGHT));
    }

    private String fileName(OnePageDraftEntity draft) {
        String suffix = draft.getId().toString().substring(0, 8);
        return "slideforge-one-page-" + suffix + ".pptx";
    }

    private UUID toUuid(String draftId) {
        if (!StringUtils.hasText(draftId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "存在尚未生成成功的页面草稿，请先重试失败页面。");
        }

        try {
            return UUID.fromString(draftId);
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "页面草稿 ID 不合法。");
        }
    }

    public record ExportedPptx(String fileName, byte[] content) {
    }
}
