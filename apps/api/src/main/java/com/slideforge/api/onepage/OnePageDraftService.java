package com.slideforge.api.onepage;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.slideforge.api.ai.AiRuntimeService;
import com.slideforge.api.ai.prompt.RenderedPrompt;
import com.slideforge.api.ai.provider.AiChatResponse;
import com.slideforge.api.onepage.dto.ConsultResponse;
import com.slideforge.api.onepage.dto.CreateOnePageDraftResponse;
import com.slideforge.api.onepage.dto.OnePageDraftResponse;
import com.slideforge.api.onepage.dto.PagePlan;
import com.slideforge.api.onepage.dto.RequirementBrief;
import com.slideforge.api.onepage.dto.ResearchPack;
import com.slideforge.api.onepage.dto.SvgGenerateResponse;
import com.slideforge.api.onepage.dto.ValidationReport;
import com.slideforge.api.onepage.dto.VisualSpec;
import com.slideforge.api.research.SearchClient;
import com.slideforge.api.research.SearchResult;
import com.slideforge.api.svg.SvgValidationService;
import com.slideforge.api.workflow.WorkflowRun;
import com.slideforge.api.workflow.WorkflowRunRepository;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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
    private final OnePagePromptService onePagePromptService;
    private final SearchClient searchClient;
    private final ObjectMapper objectMapper;

    public OnePageDraftService(
            OnePageDraftRepository onePageDraftRepository,
            WorkflowRunRepository workflowRunRepository,
            SvgValidationService svgValidationService,
            AiRuntimeService aiRuntimeService,
            OnePagePromptService onePagePromptService,
            SearchClient searchClient,
            ObjectMapper objectMapper
    ) {
        this.onePageDraftRepository = onePageDraftRepository;
        this.workflowRunRepository = workflowRunRepository;
        this.svgValidationService = svgValidationService;
        this.aiRuntimeService = aiRuntimeService;
        this.onePagePromptService = onePagePromptService;
        this.searchClient = searchClient;
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

        RenderedPrompt prompt = onePagePromptService.consultant(draft.getInitialPrompt(), message);
        AiChatResponse response = callPrompt(prompt);

        draft.setStatus("CONSULTED");
        onePageDraftRepository.save(draft);
        recordWorkflow(draft, "consult", prompt, response.content(), start, null);
        return new ConsultResponse(response.content(), true);
    }

    public RequirementBrief generateBrief(String draftId) {
        long start = System.currentTimeMillis();
        OnePageDraftEntity draft = getExistingDraft(draftId);
        RenderedPrompt prompt = onePagePromptService.brief(draft.getInitialPrompt());
        AiChatResponse response = callPrompt(prompt);
        RequirementBrief brief = parseModelJsonWithRepair(response.content(), RequirementBrief.class);

        draft.setRequirementBriefJson(toJson(brief));
        draft.setStatus("BRIEF_READY");
        onePageDraftRepository.save(draft);
        recordWorkflow(draft, "brief", prompt, toJson(brief), start, null);
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
        return generateResearch(draftId, "model-only");
    }

    public ResearchPack generateResearch(String draftId, String requestedMode) {
        long start = System.currentTimeMillis();
        OnePageDraftEntity draft = getExistingDraft(draftId);
        ensureBrief(draft);
        draft = getExistingDraft(draftId);

        String researchMode = normalizeResearchMode(requestedMode);
        List<SearchResult> sources = collectSources(draft, researchMode, start);

        if ("search-assisted".equals(researchMode) && sources.isEmpty()) {
            researchMode = "model-only";
        }

        RenderedPrompt prompt = onePagePromptService.research(
                draft.getRequirementBriefJson(),
                researchMode,
                toJson(sources)
        );
        AiChatResponse response = callPrompt(prompt);
        ResearchPack researchPack = normalizeResearchPack(parseModelJsonWithRepair(response.content(), ResearchPack.class), researchMode, sources);

        draft.setResearchPackJson(toJson(researchPack));
        draft.setStatus("RESEARCH_READY");
        onePageDraftRepository.save(draft);
        recordWorkflow(draft, "research", prompt, toJson(researchPack), start, null);
        return researchPack;
    }

    public PagePlan generatePagePlan(String draftId) {
        long start = System.currentTimeMillis();
        OnePageDraftEntity draft = getExistingDraft(draftId);
        ensureBrief(draft);
        ensureResearch(draft);
        draft = getExistingDraft(draftId);

        RenderedPrompt prompt = onePagePromptService.pagePlan(
                draft.getRequirementBriefJson(),
                draft.getResearchPackJson()
        );
        AiChatResponse response = callPrompt(prompt);
        PagePlan pagePlan = parseModelJsonWithRepair(response.content(), PagePlan.class);

        draft.setPagePlanJson(toJson(pagePlan));
        draft.setStatus("PAGE_PLAN_READY");
        onePageDraftRepository.save(draft);
        recordWorkflow(draft, "pagePlan", prompt, toJson(pagePlan), start, null);
        return pagePlan;
    }

    public PagePlan updatePagePlan(String draftId, PagePlan pagePlan) {
        OnePageDraftEntity draft = getExistingDraft(draftId);
        draft.setPagePlanJson(toJson(pagePlan));
        draft.setStatus("PAGE_PLAN_READY");
        onePageDraftRepository.save(draft);
        return pagePlan;
    }

    public VisualSpec generateVisualSpec(String draftId) {
        long start = System.currentTimeMillis();
        OnePageDraftEntity draft = getExistingDraft(draftId);

        if (draft.getPagePlanJson() == null) {
            generatePagePlan(draftId);
            draft = getExistingDraft(draftId);
        }

        RenderedPrompt prompt = onePagePromptService.visualSpec(draft.getPagePlanJson());
        AiChatResponse response = callPrompt(prompt);
        VisualSpec visualSpec = normalizeVisualSpec(parseModelJsonWithRepair(response.content(), VisualSpec.class));

        draft.setVisualSpecJson(toJson(visualSpec));
        draft.setStatus("VISUAL_SPEC_READY");
        onePageDraftRepository.save(draft);
        recordWorkflow(draft, "visualSpec", prompt, toJson(visualSpec), start, null);
        return visualSpec;
    }

    public VisualSpec updateVisualSpec(String draftId, VisualSpec visualSpec) {
        OnePageDraftEntity draft = getExistingDraft(draftId);
        VisualSpec normalized = normalizeVisualSpec(visualSpec);

        draft.setVisualSpecJson(toJson(normalized));
        draft.setStatus("VISUAL_SPEC_READY");
        onePageDraftRepository.save(draft);
        return normalized;
    }

    public SvgGenerateResponse generateSvg(String draftId) {
        long start = System.currentTimeMillis();
        OnePageDraftEntity draft = getExistingDraft(draftId);

        if (draft.getPagePlanJson() == null) {
            generatePagePlan(draftId);
            draft = getExistingDraft(draftId);
        }

        if (draft.getVisualSpecJson() == null) {
            generateVisualSpec(draftId);
            draft = getExistingDraft(draftId);
        }

        RenderedPrompt prompt = onePagePromptService.svg(draft.getPagePlanJson(), draft.getVisualSpecJson());
        AiChatResponse response = callPrompt(prompt);
        String rawSvg = extractSvg(response.content());
        String sanitizedSvg = svgValidationService.sanitize(rawSvg);
        ValidationReport validationReport = svgValidationService.validate(sanitizedSvg);

        if (!validationReport.valid()) {
            sanitizedSvg = repairSvg(sanitizedSvg);
            validationReport = svgValidationService.validate(sanitizedSvg);
        }

        draft.setSvgContent(sanitizedSvg);
        draft.setValidationReportJson(toJson(validationReport));
        draft.setStatus(validationReport.valid() ? "SVG_READY" : "FAILED");
        onePageDraftRepository.save(draft);
        recordWorkflow(draft, "svg", prompt, sanitizedSvg, start, null);
        return new SvgGenerateResponse(sanitizedSvg, validationReport);
    }

    public SvgGenerateResponse updateSvg(String draftId, String svgContent) {
        long start = System.currentTimeMillis();
        OnePageDraftEntity draft = getExistingDraft(draftId);
        String sanitizedSvg = svgValidationService.sanitize(extractSvg(svgContent));
        ValidationReport validationReport = svgValidationService.validate(sanitizedSvg);

        draft.setSvgContent(sanitizedSvg);
        draft.setValidationReportJson(toJson(validationReport));
        draft.setStatus(validationReport.valid() ? "SVG_READY" : "FAILED");
        onePageDraftRepository.save(draft);
        recordWorkflow(draft, "svgManualEdit", null, sanitizedSvg, start, null);
        return new SvgGenerateResponse(sanitizedSvg, validationReport);
    }

    private AiChatResponse callPrompt(RenderedPrompt prompt) {
        return aiRuntimeService.chat(
                LOCAL_USER_ID,
                prompt.messages(),
                prompt.responseFormat(),
                prompt.maxTokens()
        );
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

    private String normalizeResearchMode(String requestedMode) {
        if ("search-assisted".equalsIgnoreCase(requestedMode)) {
            return "search-assisted";
        }

        return "model-only";
    }

    private List<SearchResult> collectSources(OnePageDraftEntity draft, String researchMode, long start) {
        if (!"search-assisted".equals(researchMode) || !searchClient.available()) {
            return List.of();
        }

        List<String> queries = planSearchQueries(draft, start);
        Map<String, SearchResult> results = new LinkedHashMap<>();

        for (String query : queries) {
            searchClient.search(query).stream()
                    .limit(5)
                    .forEach(result -> results.putIfAbsent(result.url(), result));
        }

        return results.values().stream()
                .limit(10)
                .toList();
    }

    private List<String> planSearchQueries(OnePageDraftEntity draft, long start) {
        try {
            RenderedPrompt prompt = onePagePromptService.searchQueries(draft.getRequirementBriefJson());
            AiChatResponse response = callPrompt(prompt);
            SearchQueryPlan plan = parseModelJsonWithRepair(response.content(), SearchQueryPlan.class);
            List<String> queries = normalizeQueries(plan.queries(), draft.getRequirementBriefJson());
            recordWorkflow(draft, "searchQuery", prompt, toJson(new SearchQueryPlan(queries)), start, null);
            return queries;
        } catch (RuntimeException exception) {
            return List.of(toFallbackSearchQuery(draft.getRequirementBriefJson()));
        }
    }

    private List<String> normalizeQueries(List<String> queries, String requirementBriefJson) {
        List<String> normalized = queries == null ? List.of() : queries.stream()
                .filter(query -> query != null && !query.isBlank())
                .map(String::trim)
                .distinct()
                .limit(5)
                .toList();

        if (normalized.isEmpty()) {
            return List.of(toFallbackSearchQuery(requirementBriefJson));
        }

        return normalized;
    }

    private String toFallbackSearchQuery(String requirementBriefJson) {
        String compact = requirementBriefJson
                .replace("{", " ")
                .replace("}", " ")
                .replace("\"", " ")
                .replace(":", " ")
                .replace(",", " ")
                .replaceAll("\\s+", " ")
                .trim();

        return compact.length() <= 300 ? compact : compact.substring(0, 300);
    }

    private ResearchPack normalizeResearchPack(ResearchPack pack, String researchMode, List<SearchResult> sources) {
        List<ResearchPack.Source> normalizedSources = "search-assisted".equals(researchMode)
                ? sources.stream()
                        .map(source -> new ResearchPack.Source(
                                source.id(),
                                source.title(),
                                source.url(),
                                source.publisher(),
                                source.publishedAt(),
                                source.snippet()
                        ))
                        .toList()
                : List.of();
        List<String> limitations = pack.limitations() == null ? List.of() : pack.limitations();

        if ("model-only".equals(researchMode) && limitations.stream().noneMatch(item -> item.contains("外部来源"))) {
            limitations = new java.util.ArrayList<>(limitations);
            limitations.add("当前未接入外部搜索来源，资料由模型根据 brief 整理。");
        }

        return new ResearchPack(
                researchMode,
                pack.summary(),
                pack.keyPoints(),
                pack.evidence(),
                normalizedSources,
                limitations
        );
    }

    private OnePageDraftEntity getExistingDraft(String draftId) {
        return onePageDraftRepository.findById(UUID.fromString(draftId))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "草稿不存在。"));
    }

    private VisualSpec normalizeVisualSpec(VisualSpec visualSpec) {
        if (visualSpec == null) {
            return defaultVisualSpec();
        }

        VisualSpec.Theme theme = visualSpec.theme() == null
                ? defaultVisualSpec().theme()
                : visualSpec.theme();
        List<VisualSpec.Card> cards = visualSpec.cards() == null || visualSpec.cards().isEmpty()
                ? defaultVisualSpec().cards()
                : visualSpec.cards().stream()
                        .map(this::normalizeCard)
                        .toList();

        return new VisualSpec(
                new VisualSpec.Canvas(1280, 720, "0 0 1280 720"),
                theme,
                cards
        );
    }

    private VisualSpec.Card normalizeCard(VisualSpec.Card card) {
        int x = clamp(card.x(), 0, 1200);
        int y = clamp(card.y(), 0, 660);
        int w = clamp(card.w(), 80, 1280 - x);
        int h = clamp(card.h(), 60, 720 - y);

        return new VisualSpec.Card(
                card.id(),
                card.blockId(),
                x,
                y,
                w,
                h,
                card.priority()
        );
    }

    private int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(value, max));
    }

    private VisualSpec defaultVisualSpec() {
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

    private <T> T parseModelJsonWithRepair(String content, Class<T> type) {
        try {
            return parseModelJson(content, type);
        } catch (ResponseStatusException exception) {
            RenderedPrompt repairPrompt = onePagePromptService.jsonRepair(content);
            AiChatResponse repairResponse = callPrompt(repairPrompt);
            return parseModelJson(repairResponse.content(), type);
        }
    }

    private String repairSvg(String svgContent) {
        RenderedPrompt repairPrompt = onePagePromptService.svgRepair(svgContent);
        AiChatResponse repairResponse = callPrompt(repairPrompt);
        String repairedSvg = extractSvg(repairResponse.content());
        return svgValidationService.sanitize(repairedSvg);
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
            RenderedPrompt prompt,
            String outputJson,
            long start,
            String errorMessage
    ) {
        workflowRunRepository.save(new WorkflowRun(
                LOCAL_USER_ID,
                draft.getId(),
                stage,
                "user-configured",
                prompt == null ? "manual" : prompt.key(),
                prompt == null ? "" : prompt.renderedUserPrompt(),
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

    private record SearchQueryPlan(List<String> queries) {
    }
}
