package com.slideforge.api.onepage;

import java.awt.Dimension;
import java.awt.Rectangle;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
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

        try (XMLSlideShow ppt = new XMLSlideShow(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            ppt.setPageSize(new Dimension(SLIDE_WIDTH, SLIDE_HEIGHT));
            XSLFSlide slide = ppt.createSlide();
            XSLFPictureData pictureData = ppt.addPicture(
                    draft.getSvgContent().getBytes(),
                    PictureData.PictureType.SVG
            );
            XSLFPictureShape picture = slide.createPicture(pictureData);
            picture.setAnchor(new Rectangle(0, 0, SLIDE_WIDTH, SLIDE_HEIGHT));
            ppt.write(output);

            return new ExportedPptx(fileName(draft), output.toByteArray());
        } catch (IOException exception) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "PPTX 导出失败。");
        }
    }

    private String fileName(OnePageDraftEntity draft) {
        String suffix = draft.getId().toString().substring(0, 8);
        return "slideforge-one-page-" + suffix + ".pptx";
    }

    public record ExportedPptx(String fileName, byte[] content) {
    }
}
