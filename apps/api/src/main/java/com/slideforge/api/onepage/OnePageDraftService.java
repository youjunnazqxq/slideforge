package com.slideforge.api.onepage;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.slideforge.api.ai.AiRuntimeService;
import com.slideforge.api.ai.provider.AiChatResponse;
import com.slideforge.api.ai.provider.AiMessage;
import com.slideforge.api.onepage.dto.ConsultResponse;
import com.slideforge.api.onepage.dto.CreateOnePageDraftResponse;
import com.slideforge.api.onepage.dto.OnePageDraftResponse;
import com.slideforge.api.onepage.dto.PagePlan;
import com.slideforge.api.onepage.dto.RequirementBrief;
import com.slideforge.api.onepage.dto.ResearchPack;
import com.slideforge.api.onepage.dto.SvgGenerateResponse;
import com.slideforge.api.onepage.dto.ValidationReport;
import com.slideforge.api.onepage.dto.VisualSpec;
import com.slideforge.api.svg.SvgValidationService;
import com.slideforge.api.workflow.WorkflowRun;
import com.slideforge.api.workflow.WorkflowRunRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class OnePageDraftService {

    private static final String LOCAL_USER_ID = "local-user";

    private final OnePageDraftRepository onePageDraftRepository;
    private final WorkflowRunRepository workflowRunRepository;
    private final SvgValidationService svgValidationService;
    private final AiRuntimeService aiRuntimeService;
    private final ObjectMapper objectMapper;

    public OnePageDraftService(
            OnePageDraftRepository onePageDraftRepository,
            WorkflowRunRepository workflowRunRepository,
            SvgValidationService svgValidationService,
            AiRuntimeService aiRuntimeService,
            ObjectMapper objectMapper
    ) {
        this.onePageDraftRepository = onePageDraftRepository;
        this.workflowRunRepository = workflowRunRepository;
        this.svgValidationService = svgValidationService;
        this.aiRuntimeService = aiRuntimeService;
        this.objectMapper = objectMapper;
    }

    public CreateOnePageDraftResponse createDraft(String initialPrompt) {
        OnePageDraftEntity draft = onePageDraftRepository.save(new OnePageDraftEntity(LOCAL_USER_ID, initialPrompt));
        return new CreateOnePageDraftResponse(draft.getId().toString(), draft.getStatus());
    }

    public OnePageDraftResponse getDraft(String draftId) {
        return toResponse(getExistingDraft(draftId));
    }

    public ConsultResponse consult(String draftId, String message) {
        long start = System.currentTimeMillis();
        OnePageDraftEntity draft = getExistingDraft(draftId);
        draft.setStatus("CONSULTING");
        onePageDraftRepository.save(draft);

        String prompt = """
                你是 SlideForge 的一页 PPT 需求顾问。请根据用户输入判断信息是否足够生成结构化 brief。
                输出 2-4 句中文：先确认已理解的主题，再指出缺失信息；如果足够，请明确说可以生成 brief。

                初始需求：
                %s

                用户补充：
                %s
                """.formatted(draft.getInitialPrompt(), message);

        AiChatResponse response = aiRuntimeService.chat(
                LOCAL_USER_ID,
                List.of(new AiMessage("user", prompt)),
                "text",
                1024
        );

        draft.setStatus("CONSULTED");
        onePageDraftRepository.save(draft);
        recordWorkflow(draft, "consult", "consultant", prompt, response.content(), response, start, null);
        return new ConsultResponse(response.content(), true);
    }

    public RequirementBrief generateBrief(String draftId) {
        long start = System.currentTimeMillis();
        OnePageDraftEntity draft = getExistingDraft(draftId);
        String prompt = """
                你是产品型 PPT 策划助手。请把用户需求整理成一页 PPT 的结构化 brief。
                只返回 JSON，不要 Markdown，不要解释。

                JSON 字段必须完全如下：
                {
                  "topic": "string",
                  "audience": "string",
                  "scenario": "string",
                  "goal": "string",
                  "coreConclusion": "string",
                  "tone": "string",
                  "mustInclude": ["string"],
                  "avoid": ["string"],
                  "language": "zh-CN",
                  "canvasRatio": "16:9"
                }

                用户需求：
                %s
                """.formatted(draft.getInitialPrompt());

        AiChatResponse response = aiRuntimeService.chat(
                LOCAL_USER_ID,
                List.of(new AiMessage("user", prompt)),
                "json",
                2048
        );
        RequirementBrief brief = parseModelJson(response.content(), RequirementBrief.class);

        draft.setRequirementBriefJson(toJson(brief));
        draft.setStatus("BRIEF_READY");
        onePageDraftRepository.save(draft);
        recordWorkflow(draft, "brief", "brief.extract", prompt, toJson(brief), response, start, null);
        return brief;
    }

    public RequirementBrief updateBrief(String draftId, RequirementBrief brief) {
        OnePageDraftEntity draft = getExistingDraft(draftId);
        draft.setRequirementBriefJson(toJson(brief));
        draft.setStatus("BRIEF_READY");
        onePageDraftRepository.save(draft);
        return brief;
    }

    public ResearchPack generateResearch(String draftId) {
        long start = System.currentTimeMillis();
        OnePageDraftEntity draft = getExistingDraft(draftId);
        ensureBrief(draft);
        draft = getExistingDraft(draftId);

        String prompt = """
                你是商业与产品资料研究助手。请基于 brief 生成 model-only researchPack。
                当前没有联网搜索，所以不要编造 URL 或外部来源，sources 必须为空数组。
                只返回 JSON，不要 Markdown，不要解释。

                JSON 字段必须完全如下：
                {
                  "mode": "model-only",
                  "summary": "string",
                  "keyPoints": ["string"],
                  "evidence": [{"claim": "string", "support": "string", "sourceIds": []}],
                  "sources": [],
                  "limitations": ["string"]
                }

                brief JSON：
                %s
                """.formatted(draft.getRequirementBriefJson());

        AiChatResponse response = aiRuntimeService.chat(
                LOCAL_USER_ID,
                List.of(new AiMessage("user", prompt)),
                "json",
                3072
        );
        ResearchPack researchPack = parseModelJson(response.content(), ResearchPack.class);

        draft.setResearchPackJson(toJson(researchPack));
        draft.setStatus("RESEARCH_READY");
        onePageDraftRepository.save(draft);
        recordWorkflow(draft, "research", "research.collect", prompt, toJson(researchPack), response, start, null);
        return researchPack;
    }

    public PagePlan generatePagePlan(String draftId) {
        long start = System.currentTimeMillis();
        OnePageDraftEntity draft = getExistingDraft(draftId);
        ensureBrief(draft);
        ensureResearch(draft);
        draft = getExistingDraft(draftId);

        String prompt = """
                你是一页 PPT 信息架构师。请基于 brief 和 researchPack 生成一页 16:9 PPT 的 pagePlan。
                内容应适合 Bento Grid：一个核心结论块，2-4 个支撑块。只返回 JSON，不要 Markdown，不要解释。

                JSON 字段必须完全如下：
                {
                  "slideTitle": "string",
                  "coreMessage": "string",
                  "audienceTakeaway": "string",
                  "contentBlocks": [
                    {"id": "primary", "role": "primary", "type": "conclusion", "title": "string", "content": "string"}
                  ],
                  "layoutIntent": "string",
                  "visualStyle": "string"
                }

                brief JSON：
                %s

                researchPack JSON：
                %s
                """.formatted(draft.getRequirementBriefJson(), draft.getResearchPackJson());

        AiChatResponse response = aiRuntimeService.chat(
                LOCAL_USER_ID,
                List.of(new AiMessage("user", prompt)),
                "json",
                4096
        );
        PagePlan pagePlan = parseModelJson(response.content(), PagePlan.class);

        draft.setPagePlanJson(toJson(pagePlan));
        draft.setVisualSpecJson(toJson(generateVisualSpec()));
        draft.setStatus("PAGE_PLAN_READY");
        onePageDraftRepository.save(draft);
        recordWorkflow(draft, "pagePlan", "page-plan.generate", prompt, toJson(pagePlan), response, start, null);
        return pagePlan;
    }

    public PagePlan updatePagePlan(String draftId, PagePlan pagePlan) {
        OnePageDraftEntity draft = getExistingDraft(draftId);
        draft.setPagePlanJson(toJson(pagePlan));
        draft.setVisualSpecJson(toJson(generateVisualSpec()));
        draft.setStatus("PAGE_PLAN_READY");
        onePageDraftRepository.save(draft);
        return pagePlan;
    }

    public SvgGenerateResponse generateSvg(String draftId) {
        long start = System.currentTimeMillis();
        OnePageDraftEntity draft = getExistingDraft(draftId);

        if (draft.getPagePlanJson() == null) {
            generatePagePlan(draftId);
            draft = getExistingDraft(draftId);
        }

        String prompt = """
                你是 SVG 信息设计师。请根据 pagePlan 生成一张 16:9、viewBox="0 0 1280 720" 的单页 PPT SVG。
                严格要求：
                - 只返回 <svg>...</svg>，不要 Markdown，不要解释。
                - 禁止 script、foreignObject、外链图片、外链字体。
                - 使用专业克制的 Bento Grid 布局，文本不要重叠。
                - 背景、卡片、标题、正文必须都在 viewBox 内。

                pagePlan JSON：
                %s
                """.formatted(draft.getPagePlanJson());

        AiChatResponse response = aiRuntimeService.chat(
                LOCAL_USER_ID,
                List.of(new AiMessage("user", prompt)),
                "text",
                4096
        );
        String rawSvg = extractSvg(response.content());
        String sanitizedSvg = svgValidationService.sanitize(rawSvg);
        ValidationReport validationReport = svgValidationService.validate(sanitizedSvg);

        draft.setSvgContent(sanitizedSvg);
        draft.setValidationReportJson(toJson(validationReport));
        draft.setStatus(validationReport.valid() ? "SVG_READY" : "FAILED");
        onePageDraftRepository.save(draft);
        recordWorkflow(draft, "svg", "svg.generate", prompt, sanitizedSvg, response, start, null);
        return new SvgGenerateResponse(sanitizedSvg, validationReport);
    }

    private void ensureBrief(OnePageDraftEntity draft) {
        if (draft.getRequirementBriefJson() == null) {
            generateBrief(draft.getId().toString());
        }
    }

    private void ensureResearch(OnePageDraftEntity draft) {
        if (draft.getResearchPackJson() == null) {
            generateResearch(draft.getId().toString());
        }
    }

    private OnePageDraftEntity getExistingDraft(String draftId) {
        return onePageDraftRepository.findById(UUID.fromString(draftId))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "草稿不存在。"));
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

    private <T> T parseModelJson(String content, Class<T> type) {
        try {
            return objectMapper.readValue(extractJsonObject(content), type);
        } catch (JsonProcessingException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "AI 返回 JSON 格式不合法。");
        }
    }

    private String extractJsonObject(String content) {
        int start = content.indexOf('{');
        int end = content.lastIndexOf('}');

        if (start < 0 || end <= start) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "AI 未返回 JSON 对象。");
        }

        return content.substring(start, end + 1);
    }

    private String extractSvg(String content) {
        int start = content.indexOf("<svg");
        int end = content.lastIndexOf("</svg>");

        if (start < 0 || end <= start) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "AI 未返回 SVG。");
        }

        return content.substring(start, end + "</svg>".length());
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "JSON 序列化失败。");
        }
    }

    private <T> T fromJson(String json, Class<T> type) {
        if (json == null) {
            return null;
        }

        try {
            return objectMapper.readValue(json, type);
        } catch (JsonProcessingException exception) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "JSON 解析失败。");
        }
    }

    private void recordWorkflow(
            OnePageDraftEntity draft,
            String stage,
            String promptKey,
            String inputJson,
            String outputJson,
            AiChatResponse response,
            long start,
            String errorMessage
    ) {
        workflowRunRepository.save(new WorkflowRun(
                LOCAL_USER_ID,
                draft.getId(),
                stage,
                "user-configured",
                promptKey,
                inputJson,
                outputJson,
                errorMessage == null ? "SUCCESS" : "FAILED",
                errorMessage,
                System.currentTimeMillis() - start
        ));
    }

    private OnePageDraftResponse toResponse(OnePageDraftEntity draft) {
        return new OnePageDraftResponse(
                draft.getId().toString(),
                draft.getStatus(),
                draft.getInitialPrompt(),
                fromJson(draft.getRequirementBriefJson(), RequirementBrief.class),
                fromJson(draft.getResearchPackJson(), ResearchPack.class),
                fromJson(draft.getPagePlanJson(), PagePlan.class),
                fromJson(draft.getVisualSpecJson(), VisualSpec.class),
                draft.getSvgContent(),
                fromJson(draft.getValidationReportJson(), ValidationReport.class)
        );
    }
}
