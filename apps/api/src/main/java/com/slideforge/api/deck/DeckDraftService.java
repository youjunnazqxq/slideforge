package com.slideforge.api.deck;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.slideforge.api.ai.AiRuntimeService;
import com.slideforge.api.ai.prompt.RenderedPrompt;
import com.slideforge.api.ai.provider.AiChatResponse;
import com.slideforge.api.deck.dto.CreateDeckDraftResponse;
import com.slideforge.api.deck.dto.DeckDraftResponse;
import com.slideforge.api.deck.dto.DeckOutline;
import com.slideforge.api.deck.dto.DeckSlideDraftResponse;
import com.slideforge.api.deck.dto.SlideStickyNote;
import com.slideforge.api.onepage.OnePageDraftService;
import com.slideforge.api.onepage.OnePagePromptService;
import com.slideforge.api.onepage.dto.CreateOnePageDraftResponse;
import com.slideforge.api.onepage.dto.ResearchPack;
import com.slideforge.api.research.SearchClient;
import com.slideforge.api.research.SearchResult;
import com.slideforge.api.workflow.WorkflowRun;
import com.slideforge.api.workflow.WorkflowRunRepository;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class DeckDraftService {

    private static final String LOCAL_USER_ID = "local-user";

    private final DeckDraftRepository deckDraftRepository;
    private final DeckPromptService deckPromptService;
    private final AiRuntimeService aiRuntimeService;
    private final OnePageDraftService onePageDraftService;
    private final OnePagePromptService onePagePromptService;
    private final SearchClient searchClient;
    private final WorkflowRunRepository workflowRunRepository;
    private final ObjectMapper objectMapper;

    public DeckDraftService(
            DeckDraftRepository deckDraftRepository,
            DeckPromptService deckPromptService,
            AiRuntimeService aiRuntimeService,
            OnePageDraftService onePageDraftService,
            OnePagePromptService onePagePromptService,
            SearchClient searchClient,
            WorkflowRunRepository workflowRunRepository,
            ObjectMapper objectMapper
    ) {
        this.deckDraftRepository = deckDraftRepository;
        this.deckPromptService = deckPromptService;
        this.aiRuntimeService = aiRuntimeService;
        this.onePageDraftService = onePageDraftService;
        this.onePagePromptService = onePagePromptService;
        this.searchClient = searchClient;
        this.workflowRunRepository = workflowRunRepository;
        this.objectMapper = objectMapper;
    }

    public CreateDeckDraftResponse createDraft(String initialPrompt) {
        DeckDraftEntity draft = deckDraftRepository.save(new DeckDraftEntity(LOCAL_USER_ID, initialPrompt));
        return new CreateDeckDraftResponse(draft.getId().toString(), draft.getStatus());
    }

    public DeckDraftResponse getDraft(String deckId) {
        return toResponse(getExistingDraft(deckId));
    }

    public DeckOutline generateOutline(String deckId) {
        long start = System.currentTimeMillis();
        DeckDraftEntity draft = getExistingDraft(deckId);
        ensureResearch(draft);
        draft = getExistingDraft(deckId);
        draft.setStatus("OUTLINE_RUNNING");
        deckDraftRepository.save(draft);

        RenderedPrompt prompt = deckPromptService.outline(draft.getInitialPrompt() + "\n\nResearch pack JSON:\n" + draft.getResearchPackJson());
        AiChatResponse response = aiRuntimeService.chat(
                LOCAL_USER_ID,
                prompt.messages(),
                prompt.responseFormat(),
                prompt.maxTokens()
        );
        DeckOutline outline = normalizeOutline(parseModelJson(response.content(), DeckOutline.class), draft.getInitialPrompt());
        List<SlideStickyNote> stickyNotes = toStickyNotes(outline);

        draft.setOutlineJson(toJson(outline));
        draft.setStickyNotesJson(toJson(stickyNotes));
        draft.setStatus("OUTLINE_READY");
        deckDraftRepository.save(draft);
        recordWorkflow(draft, "deckOutline", prompt, toJson(outline), start);
        return outline;
    }

    public ResearchPack generateResearch(String deckId, String requestedMode) {
        long start = System.currentTimeMillis();
        DeckDraftEntity draft = getExistingDraft(deckId);
        String researchMode = "search-assisted".equalsIgnoreCase(requestedMode) ? "search-assisted" : "model-only";
        List<SearchResult> sources = collectDeckSources(draft, researchMode);

        if ("search-assisted".equals(researchMode) && sources.isEmpty()) {
            researchMode = "model-only";
        }

        RenderedPrompt prompt = onePagePromptService.research(deckBriefJson(draft), researchMode, toJson(sources));
        AiChatResponse response = aiRuntimeService.chat(
                LOCAL_USER_ID,
                prompt.messages(),
                prompt.responseFormat(),
                prompt.maxTokens()
        );
        ResearchPack researchPack = normalizeResearchPack(parseModelJson(response.content(), ResearchPack.class), researchMode, sources);

        draft.setResearchPackJson(toJson(researchPack));
        draft.setStatus("RESEARCH_READY");
        deckDraftRepository.save(draft);
        recordWorkflow(draft, "deckResearch", prompt, toJson(researchPack), start);
        return researchPack;
    }

    public List<SlideStickyNote> saveStickyNotes(String deckId, List<SlideStickyNote> stickyNotes) {
        DeckDraftEntity draft = getExistingDraft(deckId);
        List<SlideStickyNote> normalized = normalizeStickyNotes(stickyNotes);
        draft.setStickyNotesJson(toJson(normalized));
        draft.setStatus("STICKY_NOTES_READY");
        deckDraftRepository.save(draft);
        return normalized;
    }

    public SlideStickyNote addStickyNote(String deckId, SlideStickyNote stickyNote) {
        DeckDraftEntity draft = getExistingDraft(deckId);
        List<SlideStickyNote> notes = new ArrayList<>(stickyNotesFromJson(draft.getStickyNotesJson()));
        SlideStickyNote note = new SlideStickyNote(
                hasText(stickyNote.slideId()) ? stickyNote.slideId() : "slide-" + UUID.randomUUID().toString().substring(0, 8),
                notes.size() + 1,
                stickyNote.sectionTitle(),
                stickyNote.title(),
                stickyNote.message(),
                "planned",
                stickyNote.tags() == null ? List.of("content") : stickyNote.tags()
        );
        notes.add(note);
        draft.setStickyNotesJson(toJson(normalizeStickyNotes(notes)));
        draft.setStatus("STICKY_NOTES_READY");
        deckDraftRepository.save(draft);
        return note;
    }

    public List<SlideStickyNote> deleteStickyNote(String deckId, String slideId) {
        DeckDraftEntity draft = getExistingDraft(deckId);
        List<SlideStickyNote> normalized = normalizeStickyNotes(stickyNotesFromJson(draft.getStickyNotesJson()).stream()
                .filter(note -> !note.slideId().equals(slideId))
                .toList());
        draft.setStickyNotesJson(toJson(normalized));
        draft.setStatus("STICKY_NOTES_READY");
        deckDraftRepository.save(draft);
        return normalized;
    }

    public CreateOnePageDraftResponse createOnePageDraftFromSlide(String deckId, String slideId) {
        DeckDraftEntity draft = getExistingDraft(deckId);
        DeckOutline outline = fromJson(draft.getOutlineJson(), DeckOutline.class);

        if (outline == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "请先生成完整大纲。");
        }

        SlideStickyNote note = stickyNotesFromJson(draft.getStickyNotesJson()).stream()
                .filter(item -> item.slideId().equals(slideId))
                .findFirst()
                .orElseGet(() -> stickyNoteFromOutline(outline, slideId));

        String prompt = """
                请基于完整 PPT 大纲中的单页便利贴，生成这一页的一页 PPT。

                Deck 标题：%s
                Deck 核心论点：%s
                当前页章节：%s
                当前页标题：%s
                当前页核心信息：%s
                当前页标签：%s
                """.formatted(
                outline.title(),
                outline.coreThesis(),
                note.sectionTitle(),
                note.title(),
                note.message(),
                String.join(", ", note.tags() == null ? List.of() : note.tags())
        );
        prompt = prompt + """

                Page type guidance:
                - cover: create a title-led opening slide with deck title, thesis, audience/scenario, and restrained visual framing.
                - agenda: create a navigational slide that previews the chapter sequence, not a dense content page.
                - section: create a chapter divider with section title, purpose, and a short transition message.
                - content: create a normal evidence/argument slide with one main message and supporting blocks.
                - summary: create a closing slide with final conclusion, decision, and concrete next steps.
                """;

        return onePageDraftService.createDraft(prompt);
    }

    public List<DeckSlideDraftResponse> createOnePageDraftsFromSlides(String deckId) {
        List<DeckSlideDraftResponse> existingDrafts = generatedDraftsFromJson(getExistingDraft(deckId).getGeneratedDraftsJson());
        List<DeckSlideDraftResponse> generatedDrafts = orderedStickyNotes(deckId).stream()
                .map(note -> {
                    DeckSlideDraftResponse existingDraft = existingDrafts.stream()
                            .filter(draft -> draft.slideId().equals(note.slideId()) && hasText(draft.draftId()))
                            .findFirst()
                            .orElse(null);

                    if (existingDraft != null) {
                        return new DeckSlideDraftResponse(
                                note.slideId(),
                                note.order(),
                                note.title(),
                                existingDraft.draftId(),
                                existingDraft.status(),
                                existingDraft.errorMessage()
                        );
                    }

                    CreateOnePageDraftResponse response = createOnePageDraftFromSlide(deckId, note.slideId());
                    return toDeckSlideDraft(note, response);
                })
                .toList();
        saveGeneratedDrafts(deckId, generatedDrafts, "ONE_PAGE_DRAFTS_READY");
        return generatedDrafts;
    }

    public List<DeckSlideDraftResponse> createPagePlanDraftsFromSlides(String deckId) {
        List<DeckSlideDraftResponse> existingDrafts = generatedDraftsFromJson(getExistingDraft(deckId).getGeneratedDraftsJson());
        List<DeckSlideDraftResponse> generatedDrafts = orderedStickyNotes(deckId).stream()
                .map(note -> createPagePlanDraftForNote(deckId, note, existingDrafts))
                .toList();
        String status = generatedDrafts.stream().anyMatch(draft -> "FAILED".equals(draft.status()))
                ? "PAGE_PLANS_PARTIAL"
                : "PAGE_PLANS_READY";
        saveGeneratedDrafts(deckId, generatedDrafts, status);
        return generatedDrafts;
    }

    public List<DeckSlideDraftResponse> createVisualSpecDraftsFromSlides(String deckId) {
        List<DeckSlideDraftResponse> existingDrafts = generatedDraftsFromJson(getExistingDraft(deckId).getGeneratedDraftsJson());
        List<DeckSlideDraftResponse> generatedDrafts = orderedStickyNotes(deckId).stream()
                .map(note -> createVisualSpecDraftForNote(deckId, note, existingDrafts))
                .toList();
        String status = generatedDrafts.stream().anyMatch(draft -> "FAILED".equals(draft.status()))
                ? "VISUAL_SPECS_PARTIAL"
                : "VISUAL_SPECS_READY";
        saveGeneratedDrafts(deckId, generatedDrafts, status);
        return generatedDrafts;
    }

    public List<DeckSlideDraftResponse> createSvgDraftsFromSlides(String deckId) {
        List<DeckSlideDraftResponse> existingDrafts = generatedDraftsFromJson(getExistingDraft(deckId).getGeneratedDraftsJson());
        List<DeckSlideDraftResponse> generatedDrafts = orderedStickyNotes(deckId).stream()
                .map(note -> createSvgDraftForNote(deckId, note, existingDrafts))
                .toList();
        String status = generatedDrafts.stream().anyMatch(draft -> "FAILED".equals(draft.status()))
                ? "SLIDE_SVGS_PARTIAL"
                : "SLIDE_SVGS_READY";
        saveGeneratedDrafts(deckId, generatedDrafts, status);
        return generatedDrafts;
    }

    public DeckSlideDraftResponse createSvgDraftFromSlide(String deckId, String slideId) {
        SlideStickyNote note = orderedStickyNotes(deckId).stream()
                .filter(item -> item.slideId().equals(slideId))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "页面不存在。"));
        List<DeckSlideDraftResponse> existingDrafts = generatedDraftsFromJson(getExistingDraft(deckId).getGeneratedDraftsJson());
        DeckSlideDraftResponse response = createSvgDraftForNote(deckId, note, existingDrafts);
        List<DeckSlideDraftResponse> nextDrafts = upsertGeneratedDraft(existingDrafts, response);
        String status = nextDrafts.stream().anyMatch(draft -> "FAILED".equals(draft.status()))
                ? "SLIDE_SVGS_PARTIAL"
                : "SLIDE_SVGS_READY";
        saveGeneratedDrafts(deckId, nextDrafts, status);
        return response;
    }

    private DeckSlideDraftResponse createPagePlanDraftForNote(
            String deckId,
            SlideStickyNote note,
            List<DeckSlideDraftResponse> existingDrafts
    ) {
        String draftId = existingDrafts.stream()
                .filter(draft -> draft.slideId().equals(note.slideId()) && hasText(draft.draftId()))
                .map(DeckSlideDraftResponse::draftId)
                .findFirst()
                .orElse("");

        try {
            if (!hasText(draftId)) {
                draftId = createOnePageDraftFromSlide(deckId, note.slideId()).draftId();
            }

            onePageDraftService.generatePagePlan(draftId);
            return new DeckSlideDraftResponse(note.slideId(), note.order(), note.title(), draftId, "PAGE_PLAN_READY", null);
        } catch (RuntimeException exception) {
            return new DeckSlideDraftResponse(
                    note.slideId(),
                    note.order(),
                    note.title(),
                    draftId,
                    "FAILED",
                    errorMessage(exception)
            );
        }
    }

    private DeckSlideDraftResponse createVisualSpecDraftForNote(
            String deckId,
            SlideStickyNote note,
            List<DeckSlideDraftResponse> existingDrafts
    ) {
        String draftId = existingDrafts.stream()
                .filter(draft -> draft.slideId().equals(note.slideId()) && hasText(draft.draftId()))
                .map(DeckSlideDraftResponse::draftId)
                .findFirst()
                .orElse("");

        try {
            if (!hasText(draftId)) {
                draftId = createOnePageDraftFromSlide(deckId, note.slideId()).draftId();
            }

            onePageDraftService.generateVisualSpec(draftId);
            return new DeckSlideDraftResponse(note.slideId(), note.order(), note.title(), draftId, "VISUAL_SPEC_READY", null);
        } catch (RuntimeException exception) {
            return new DeckSlideDraftResponse(
                    note.slideId(),
                    note.order(),
                    note.title(),
                    draftId,
                    "FAILED",
                    errorMessage(exception)
            );
        }
    }

    private DeckSlideDraftResponse createSvgDraftForNote(
            String deckId,
            SlideStickyNote note,
            List<DeckSlideDraftResponse> existingDrafts
    ) {
        String draftId = existingDrafts.stream()
                .filter(draft -> draft.slideId().equals(note.slideId()) && hasText(draft.draftId()))
                .map(DeckSlideDraftResponse::draftId)
                .findFirst()
                .orElse("");

        try {
            if (!hasText(draftId)) {
                draftId = createOnePageDraftFromSlide(deckId, note.slideId()).draftId();
            }

            onePageDraftService.generateSvg(draftId);
            return new DeckSlideDraftResponse(note.slideId(), note.order(), note.title(), draftId, "SVG_READY", null);
        } catch (RuntimeException exception) {
            return new DeckSlideDraftResponse(
                    note.slideId(),
                    note.order(),
                    note.title(),
                    draftId,
                    "FAILED",
                    errorMessage(exception)
            );
        }
    }

    private List<DeckSlideDraftResponse> upsertGeneratedDraft(
            List<DeckSlideDraftResponse> generatedDrafts,
            DeckSlideDraftResponse nextDraft
    ) {
        List<DeckSlideDraftResponse> nextDrafts = new ArrayList<>();
        boolean replaced = false;

        for (DeckSlideDraftResponse draft : generatedDrafts) {
            if (draft.slideId().equals(nextDraft.slideId())) {
                nextDrafts.add(nextDraft);
                replaced = true;
            } else {
                nextDrafts.add(draft);
            }
        }

        if (!replaced) {
            nextDrafts.add(nextDraft);
        }

        return nextDrafts.stream()
                .sorted(java.util.Comparator.comparingInt(DeckSlideDraftResponse::order))
                .toList();
    }

    private void saveGeneratedDrafts(String deckId, List<DeckSlideDraftResponse> generatedDrafts, String status) {
        DeckDraftEntity draft = getExistingDraft(deckId);
        draft.setGeneratedDraftsJson(toJson(generatedDrafts));
        draft.setStatus(status);
        deckDraftRepository.save(draft);
    }

    private DeckSlideDraftResponse toDeckSlideDraft(SlideStickyNote note, CreateOnePageDraftResponse response) {
        return new DeckSlideDraftResponse(
                note.slideId(),
                note.order(),
                note.title(),
                response.draftId(),
                response.status(),
                null
        );
    }

    private List<SlideStickyNote> orderedStickyNotes(String deckId) {
        DeckDraftEntity draft = getExistingDraft(deckId);
        DeckOutline outline = fromJson(draft.getOutlineJson(), DeckOutline.class);

        if (outline == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "请先生成完整大纲。");
        }

        List<SlideStickyNote> notes = stickyNotesFromJson(draft.getStickyNotesJson());

        if (notes.isEmpty()) {
            notes = toStickyNotes(outline);
        }

        if (notes.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "没有可生成的页面便利贴。");
        }

        return notes.stream()
                .sorted(java.util.Comparator.comparingInt(SlideStickyNote::order))
                .toList();
    }

    private void ensureResearch(DeckDraftEntity draft) {
        if (draft.getResearchPackJson() == null) {
            generateResearch(draft.getId().toString(), "model-only");
        }
    }

    private List<SearchResult> collectDeckSources(DeckDraftEntity draft, String researchMode) {
        if (!"search-assisted".equals(researchMode) || !searchClient.available()) {
            return List.of();
        }

        Map<String, SearchResult> results = new LinkedHashMap<>();
        for (String query : planDeckSearchQueries(draft)) {
            searchClient.search(query).stream()
                    .limit(5)
                    .forEach(result -> results.putIfAbsent(result.url(), result));
        }
        return results.values().stream()
                .limit(8)
                .toList();
    }

    private List<String> planDeckSearchQueries(DeckDraftEntity draft) {
        try {
            RenderedPrompt prompt = onePagePromptService.searchQueries(deckBriefJson(draft));
            AiChatResponse response = aiRuntimeService.chat(
                    LOCAL_USER_ID,
                    prompt.messages(),
                    prompt.responseFormat(),
                    prompt.maxTokens()
            );
            SearchQueryPlan plan = parseModelJson(response.content(), SearchQueryPlan.class);
            List<String> queries = plan.queries() == null ? List.of() : plan.queries().stream()
                    .filter(this::hasText)
                    .map(String::trim)
                    .distinct()
                    .limit(5)
                    .toList();
            return queries.isEmpty() ? List.of(draft.getInitialPrompt()) : queries;
        } catch (RuntimeException exception) {
            return List.of(draft.getInitialPrompt());
        }
    }

    private String deckBriefJson(DeckDraftEntity draft) {
        return toJson(Map.of(
                "topic", draft.getInitialPrompt(),
                "audience", "",
                "scenario", "",
                "goal", "Create a complete presentation deck.",
                "coreConclusion", "",
                "tone", "professional",
                "mustInclude", List.of(),
                "avoid", List.of(),
                "language", "zh-CN",
                "canvasRatio", "16:9"
        ));
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

        if ("model-only".equals(researchMode) && limitations.stream().noneMatch(item -> item.contains("model-only"))) {
            limitations = new ArrayList<>(limitations);
            limitations.add("model-only research pack; no external sources were attached.");
        }

        return new ResearchPack(
                researchMode,
                pack.summary(),
                pack.keyPoints() == null ? List.of() : pack.keyPoints(),
                pack.evidence() == null ? List.of() : pack.evidence(),
                normalizedSources,
                limitations
        );
    }

    private DeckOutline normalizeOutline(DeckOutline outline, String initialPrompt) {
        String title = hasText(outline.title()) ? outline.title() : fallbackTitle(initialPrompt);
        String audience = hasText(outline.audience()) ? outline.audience() : "internal audience";
        String scenario = hasText(outline.scenario()) ? outline.scenario() : "business presentation";
        String coreThesis = hasText(outline.coreThesis()) ? outline.coreThesis() : title;
        List<DeckOutline.Section> sections = normalizeSections(outline.structure());
        List<DeckOutline.Slide> sourceSlides = outline.slides() == null ? List.of() : outline.slides();
        List<DeckOutline.Slide> normalizedSlides = new ArrayList<>();

        normalizedSlides.add(firstSlideByType(sourceSlides, "cover")
                .orElse(new DeckOutline.Slide("", "cover", sections.getFirst().id(), title, coreThesis, "Open the presentation and frame the decision.")));
        normalizedSlides.add(firstSlideByType(sourceSlides, "agenda")
                .orElse(new DeckOutline.Slide("", "agenda", sections.getFirst().id(), "Agenda", agendaMessage(sections), "Preview the narrative flow.")));

        for (DeckOutline.Section section : sections) {
            normalizedSlides.add(firstSectionSlide(sourceSlides, section.id())
                    .orElse(new DeckOutline.Slide("", "section", section.id(), section.title(), section.purpose(), "Mark the chapter transition.")));
            sourceSlides.stream()
                    .filter(slide -> section.id().equals(slide.sectionId()))
                    .filter(slide -> "content".equals(normalizeSlideType(slide.type())))
                    .forEach(normalizedSlides::add);
        }

        sourceSlides.stream()
                .filter(slide -> slide.sectionId() == null || sections.stream().noneMatch(section -> section.id().equals(slide.sectionId())))
                .filter(slide -> "content".equals(normalizeSlideType(slide.type())))
                .forEach(normalizedSlides::add);
        normalizedSlides.add(firstSlideByType(sourceSlides, "summary")
                .orElse(new DeckOutline.Slide("", "summary", sections.getLast().id(), "Summary and next steps", coreThesis, "Close with the decision and next action.")));

        return new DeckOutline(title, audience, scenario, coreThesis, sections, renumberSlides(normalizedSlides, sections));
    }

    private List<DeckOutline.Section> normalizeSections(List<DeckOutline.Section> sections) {
        List<DeckOutline.Section> source = sections == null ? List.of() : sections;
        List<DeckOutline.Section> normalized = new ArrayList<>();

        for (int index = 0; index < source.size(); index++) {
            DeckOutline.Section section = source.get(index);
            String id = hasText(section.id()) ? section.id() : "section-" + (index + 1);
            normalized.add(new DeckOutline.Section(
                    id,
                    hasText(section.title()) ? section.title() : "Section " + (index + 1),
                    hasText(section.purpose()) ? section.purpose() : "Advance the deck narrative."
            ));
        }

        if (normalized.isEmpty()) {
            normalized.add(new DeckOutline.Section("section-1", "Main story", "Present the core argument."));
        }

        return normalized;
    }

    private java.util.Optional<DeckOutline.Slide> firstSlideByType(List<DeckOutline.Slide> slides, String type) {
        return slides.stream()
                .filter(slide -> type.equals(normalizeSlideType(slide.type())))
                .findFirst();
    }

    private java.util.Optional<DeckOutline.Slide> firstSectionSlide(List<DeckOutline.Slide> slides, String sectionId) {
        return slides.stream()
                .filter(slide -> sectionId.equals(slide.sectionId()))
                .filter(slide -> "section".equals(normalizeSlideType(slide.type())))
                .findFirst();
    }

    private List<DeckOutline.Slide> renumberSlides(List<DeckOutline.Slide> slides, List<DeckOutline.Section> sections) {
        List<DeckOutline.Slide> normalized = new ArrayList<>();

        for (int index = 0; index < slides.size(); index++) {
            DeckOutline.Slide slide = slides.get(index);
            String fallbackSectionId = sections.get(Math.min(index, sections.size() - 1)).id();
            normalized.add(new DeckOutline.Slide(
                    "slide-" + String.format("%03d", index + 1),
                    normalizeSlideType(slide.type()),
                    hasText(slide.sectionId()) ? slide.sectionId() : fallbackSectionId,
                    hasText(slide.title()) ? slide.title() : "Page " + (index + 1),
                    hasText(slide.message()) ? slide.message() : "",
                    hasText(slide.purpose()) ? slide.purpose() : "Support the deck narrative."
            ));
        }

        return normalized;
    }

    private String normalizeSlideType(String type) {
        if (!hasText(type)) {
            return "content";
        }

        String normalized = type.trim().toLowerCase();

        return switch (normalized) {
            case "cover", "title" -> "cover";
            case "agenda", "toc", "table-of-contents", "table_of_contents", "directory" -> "agenda";
            case "section", "chapter", "divider", "transition" -> "section";
            case "summary", "end", "ending", "closing", "next-steps", "next_steps" -> "summary";
            default -> "content";
        };
    }

    private String agendaMessage(List<DeckOutline.Section> sections) {
        return sections.stream()
                .map(DeckOutline.Section::title)
                .filter(this::hasText)
                .limit(5)
                .collect(java.util.stream.Collectors.joining(" / "));
    }

    private String fallbackTitle(String initialPrompt) {
        if (!hasText(initialPrompt)) {
            return "Untitled deck";
        }

        return initialPrompt.length() <= 48 ? initialPrompt : initialPrompt.substring(0, 48);
    }

    private SlideStickyNote stickyNoteFromOutline(DeckOutline outline, String slideId) {
        DeckOutline.Slide slide = outline.slides().stream()
                .filter(item -> item.id().equals(slideId))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "页面不存在。"));
        return new SlideStickyNote(
                slide.id(),
                1,
                sectionTitle(outline, slide.sectionId()),
                slide.title(),
                slide.message(),
                "planned",
                List.of(slide.type())
        );
    }

    private List<SlideStickyNote> toStickyNotes(DeckOutline outline) {
        List<SlideStickyNote> notes = new ArrayList<>();

        for (int index = 0; index < outline.slides().size(); index++) {
            DeckOutline.Slide slide = outline.slides().get(index);
            notes.add(new SlideStickyNote(
                    slide.id(),
                    index + 1,
                    sectionTitle(outline, slide.sectionId()),
                    slide.title(),
                    slide.message(),
                    "planned",
                    List.of(slide.type())
            ));
        }

        return notes;
    }

    private List<SlideStickyNote> normalizeStickyNotes(List<SlideStickyNote> stickyNotes) {
        List<SlideStickyNote> normalized = new ArrayList<>();

        for (int index = 0; index < stickyNotes.size(); index++) {
            SlideStickyNote note = stickyNotes.get(index);
            normalized.add(new SlideStickyNote(
                    note.slideId(),
                    index + 1,
                    note.sectionTitle(),
                    note.title(),
                    note.message(),
                    hasText(note.status()) ? note.status() : "planned",
                    note.tags() == null ? List.of() : note.tags()
            ));
        }

        return normalized;
    }

    private String sectionTitle(DeckOutline outline, String sectionId) {
        if (outline.structure() == null) {
            return "";
        }

        return outline.structure().stream()
                .filter(section -> section.id().equals(sectionId))
                .map(DeckOutline.Section::title)
                .findFirst()
                .orElse("");
    }

    private DeckDraftEntity getExistingDraft(String deckId) {
        return deckDraftRepository.findById(UUID.fromString(deckId))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Deck 草稿不存在。"));
    }

    private <T> T parseModelJson(String content, Class<T> type) {
        try {
            return objectMapper.readValue(extractJsonObject(content), type);
        } catch (JsonProcessingException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "AI 返回大纲 JSON 格式不合法。");
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

    private List<SlideStickyNote> stickyNotesFromJson(String json) {
        if (json == null) {
            return List.of();
        }

        try {
            return objectMapper.readValue(json, new TypeReference<>() {});
        } catch (JsonProcessingException exception) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "便利贴 JSON 解析失败。");
        }
    }

    private void recordWorkflow(DeckDraftEntity draft, String stage, RenderedPrompt prompt, String outputJson, long start) {
        workflowRunRepository.save(new WorkflowRun(
                LOCAL_USER_ID,
                draft.getId(),
                stage,
                "user-configured",
                prompt.key(),
                prompt.renderedUserPrompt(),
                outputJson,
                "SUCCESS",
                null,
                System.currentTimeMillis() - start
        ));
    }

    private DeckDraftResponse toResponse(DeckDraftEntity draft) {
        return new DeckDraftResponse(
                draft.getId().toString(),
                draft.getStatus(),
                draft.getInitialPrompt(),
                fromJson(draft.getResearchPackJson(), ResearchPack.class),
                fromJson(draft.getOutlineJson(), DeckOutline.class),
                stickyNotesFromJson(draft.getStickyNotesJson()),
                generatedDraftsFromJson(draft.getGeneratedDraftsJson())
        );
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private String errorMessage(RuntimeException exception) {
        if (exception instanceof ResponseStatusException responseStatusException && hasText(responseStatusException.getReason())) {
            return responseStatusException.getReason();
        }

        return hasText(exception.getMessage()) ? exception.getMessage() : "页面生成失败，请重试。";
    }

    private List<DeckSlideDraftResponse> generatedDraftsFromJson(String json) {
        if (json == null) {
            return List.of();
        }

        try {
            return objectMapper.readValue(json, new TypeReference<>() {});
        } catch (JsonProcessingException exception) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "生成草稿 JSON 解析失败。");
        }
    }

    private record SearchQueryPlan(List<String> queries) {
    }
}
