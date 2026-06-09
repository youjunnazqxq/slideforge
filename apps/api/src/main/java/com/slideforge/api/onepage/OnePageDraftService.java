package com.slideforge.api.onepage;

import com.slideforge.api.onepage.dto.ConsultResponse;
import com.slideforge.api.onepage.dto.CreateOnePageDraftResponse;
import com.slideforge.api.onepage.dto.OnePageDraftResponse;
import com.slideforge.api.onepage.dto.PagePlan;
import com.slideforge.api.onepage.dto.RequirementBrief;
import com.slideforge.api.onepage.dto.ResearchPack;
import com.slideforge.api.onepage.dto.SvgGenerateResponse;
import com.slideforge.api.onepage.dto.ValidationReport;
import com.slideforge.api.onepage.dto.VisualSpec;
import com.slideforge.api.onepage.model.OnePageDraft;
import com.slideforge.api.svg.SvgValidationService;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class OnePageDraftService {

    private final Map<String, OnePageDraft> drafts = new ConcurrentHashMap<>();
    private final SvgValidationService svgValidationService;

    public OnePageDraftService(SvgValidationService svgValidationService) {
        this.svgValidationService = svgValidationService;
    }

    public CreateOnePageDraftResponse createDraft(String initialPrompt) {
        String id = UUID.randomUUID().toString();
        OnePageDraft draft = new OnePageDraft(id, initialPrompt);
        drafts.put(id, draft);
        return new CreateOnePageDraftResponse(id, draft.getStatus());
    }

    public OnePageDraftResponse getDraft(String draftId) {
        return toResponse(getExistingDraft(draftId));
    }

    public ConsultResponse consult(String draftId, String message) {
        OnePageDraft draft = getExistingDraft(draftId);
        draft.setStatus("CONSULTING");
        String response = "已收到补充信息。下一步可以生成结构化 brief，明确受众、场景、目标、核心结论和必须包含的信息。";
        return new ConsultResponse(response, true);
    }

    public RequirementBrief generateBrief(String draftId) {
        OnePageDraft draft = getExistingDraft(draftId);
        RequirementBrief brief = new RequirementBrief(
                "AI PPT Agent 项目可行性",
                "团队内部成员",
                "项目立项讨论",
                "帮助团队判断是否值得投入 MVP 开发",
                "项目技术上可行，但第一阶段应聚焦一页闭环，先验证工作流质量。",
                "专业、务实、信息密度适中",
                List.of("技术可行性", "产品价值", "主要风险", "下一步建议"),
                List.of("夸大商业价值", "承诺完全自动生成高质量 PPT"),
                "zh-CN",
                "16:9"
        );
        draft.setRequirementBrief(brief);
        draft.setStatus("BRIEF_READY");
        return brief;
    }

    public RequirementBrief updateBrief(String draftId, RequirementBrief brief) {
        OnePageDraft draft = getExistingDraft(draftId);
        draft.setRequirementBrief(brief);
        draft.setStatus("BRIEF_READY");
        return brief;
    }

    public ResearchPack generateResearch(String draftId) {
        OnePageDraft draft = getExistingDraft(draftId);
        ensureBrief(draft);
        ResearchPack researchPack = new ResearchPack(
                "model-only",
                "工作流型 AI PPT 的关键价值在于把需求澄清、内容策划和视觉生成拆成可编辑阶段，而不是直接套模板。",
                List.of(
                        "一页 MVP 可以先验证需求对话、brief、策划稿和 SVG 生成质量。",
                        "Bento Grid 适合承载结论、风险、能力和下一步等多块信息。",
                        "主要风险集中在模型输出稳定性、SVG 重叠和资料可靠性。"
                ),
                List.of(new ResearchPack.Evidence(
                        "分阶段生成更可控",
                        "用户可以在 brief、策划稿和 SVG 阶段介入修改。",
                        List.of()
                )),
                List.of(),
                List.of("当前为 model-only 资料整理，尚未接入外部来源。")
        );
        draft.setResearchPack(researchPack);
        draft.setStatus("RESEARCH_READY");
        return researchPack;
    }

    public PagePlan generatePagePlan(String draftId) {
        OnePageDraft draft = getExistingDraft(draftId);
        ensureBrief(draft);
        PagePlan pagePlan = new PagePlan(
                "AI PPT Agent：先验证一页闭环的可行性",
                "项目技术可行，但第一阶段应聚焦一页流程，先验证工作流质量。",
                "团队应先投入 MVP，而不是一开始做完整 PPT SaaS。",
                List.of(
                        new PagePlan.ContentBlock(
                                "primary",
                                "primary",
                                "conclusion",
                                "核心判断",
                                "一页闭环可行性高，适合作为第一阶段目标。"
                        ),
                        new PagePlan.ContentBlock(
                                "byok",
                                "supporting",
                                "list",
                                "BYOK 接入",
                                "用户使用自己的 OpenAI-compatible API，由后端代理调用。"
                        ),
                        new PagePlan.ContentBlock(
                                "next",
                                "next_step",
                                "recommendation",
                                "下一步",
                                "稳定一页闭环后，再扩展多页便利贴和 PPTX 导出。"
                        )
                ),
                "使用 Bento Grid：左侧大卡片放核心判断，右侧三张卡片放 BYOK、风险和下一步。",
                "专业、克制、现代，避免花哨装饰，强调层级和留白。"
        );
        draft.setPagePlan(pagePlan);
        draft.setStatus("PAGE_PLAN_READY");
        draft.setVisualSpec(generateVisualSpec());
        return pagePlan;
    }

    public PagePlan updatePagePlan(String draftId, PagePlan pagePlan) {
        OnePageDraft draft = getExistingDraft(draftId);
        draft.setPagePlan(pagePlan);
        draft.setVisualSpec(generateVisualSpec());
        draft.setStatus("PAGE_PLAN_READY");
        return pagePlan;
    }

    public SvgGenerateResponse generateSvg(String draftId) {
        OnePageDraft draft = getExistingDraft(draftId);
        if (draft.getPagePlan() == null) {
            generatePagePlan(draftId);
        }

        String rawSvg = buildSvg(draft.getPagePlan());
        String sanitizedSvg = svgValidationService.sanitize(rawSvg);
        ValidationReport validationReport = svgValidationService.validate(sanitizedSvg);

        draft.setSvgContent(sanitizedSvg);
        draft.setValidationReport(validationReport);
        draft.setStatus(validationReport.valid() ? "SVG_READY" : "FAILED");

        return new SvgGenerateResponse(sanitizedSvg, validationReport);
    }

    private OnePageDraft getExistingDraft(String draftId) {
        OnePageDraft draft = drafts.get(draftId);
        if (draft == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "草稿不存在");
        }
        return draft;
    }

    private void ensureBrief(OnePageDraft draft) {
        if (draft.getRequirementBrief() == null) {
            draft.setRequirementBrief(generateBrief(draft.getId()));
        }
    }

    private VisualSpec generateVisualSpec() {
        return new VisualSpec(
                new VisualSpec.Canvas(1280, 720, "0 0 1280 720"),
                new VisualSpec.Theme("#F7F8FA", "#2563EB", "#111827", "#6B7280", "#FFFFFF", "#E5E7EB"),
                List.of(
                        new VisualSpec.Card("hero", "primary", 64, 112, 560, 480, "primary"),
                        new VisualSpec.Card("byok", "byok", 656, 112, 560, 145, "secondary"),
                        new VisualSpec.Card("risk", "risk", 656, 287, 560, 145, "secondary"),
                        new VisualSpec.Card("next", "next", 656, 462, 560, 130, "secondary")
                )
        );
    }

    private String buildSvg(PagePlan pagePlan) {
        List<PagePlan.ContentBlock> blocks = new ArrayList<>(pagePlan.contentBlocks());
        String primaryContent = blocks.isEmpty() ? pagePlan.coreMessage() : blocks.getFirst().content();

        return """
                <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 1280 720" width="1280" height="720">
                  <rect width="1280" height="720" fill="#F7F8FA"/>
                  <rect x="56" y="56" width="1168" height="608" rx="24" fill="#FFFFFF" stroke="#E5E7EB"/>
                  <text x="92" y="118" fill="#2563EB" font-size="24" font-family="Arial, sans-serif" font-weight="700">SlideForge One Page</text>
                  <text x="92" y="172" fill="#111827" font-size="38" font-family="Arial, sans-serif" font-weight="700">%s</text>
                  <text x="92" y="226" fill="#4B5563" font-size="22" font-family="Arial, sans-serif">%s</text>
                  <rect x="92" y="292" width="520" height="280" rx="18" fill="#EFF6FF"/>
                  <text x="128" y="354" fill="#1D4ED8" font-size="28" font-family="Arial, sans-serif" font-weight="700">核心判断</text>
                  <text x="128" y="414" fill="#111827" font-size="24" font-family="Arial, sans-serif">%s</text>
                  <rect x="652" y="292" width="240" height="130" rx="18" fill="#ECFDF5"/>
                  <text x="684" y="345" fill="#047857" font-size="22" font-family="Arial, sans-serif" font-weight="700">BYOK</text>
                  <text x="684" y="384" fill="#111827" font-size="18" font-family="Arial, sans-serif">用户接入自己的 API</text>
                  <rect x="928" y="292" width="240" height="130" rx="18" fill="#F5F3FF"/>
                  <text x="960" y="345" fill="#7C3AED" font-size="22" font-family="Arial, sans-serif" font-weight="700">Bento Grid</text>
                  <text x="960" y="384" fill="#111827" font-size="18" font-family="Arial, sans-serif">内容驱动布局</text>
                  <rect x="652" y="442" width="516" height="130" rx="18" fill="#FFF7ED"/>
                  <text x="684" y="495" fill="#C2410C" font-size="22" font-family="Arial, sans-serif" font-weight="700">下一步</text>
                  <text x="684" y="534" fill="#111827" font-size="18" font-family="Arial, sans-serif">稳定一页闭环后，再扩展多页便利贴和 PPTX 导出。</text>
                </svg>
                """.formatted(
                escapeXml(trimForSvg(pagePlan.slideTitle(), 24)),
                escapeXml(trimForSvg(pagePlan.coreMessage(), 42)),
                escapeXml(trimForSvg(primaryContent, 28))
        );
    }

    private String trimForSvg(String value, int maxLength) {
        if (value == null) {
            return "";
        }
        if (value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength - 1) + "…";
    }

    private String escapeXml(String value) {
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;");
    }

    private OnePageDraftResponse toResponse(OnePageDraft draft) {
        return new OnePageDraftResponse(
                draft.getId(),
                draft.getStatus(),
                draft.getInitialPrompt(),
                draft.getRequirementBrief(),
                draft.getResearchPack(),
                draft.getPagePlan(),
                draft.getVisualSpec(),
                draft.getSvgContent(),
                draft.getValidationReport()
        );
    }
}
