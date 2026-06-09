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
import com.slideforge.api.deck.dto.SlideStickyNote;
import com.slideforge.api.workflow.WorkflowRun;
import com.slideforge.api.workflow.WorkflowRunRepository;
import java.util.ArrayList;
import java.util.List;
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
    private final WorkflowRunRepository workflowRunRepository;
    private final ObjectMapper objectMapper;

    public DeckDraftService(
            DeckDraftRepository deckDraftRepository,
            DeckPromptService deckPromptService,
            AiRuntimeService aiRuntimeService,
            WorkflowRunRepository workflowRunRepository,
            ObjectMapper objectMapper
    ) {
        this.deckDraftRepository = deckDraftRepository;
        this.deckPromptService = deckPromptService;
        this.aiRuntimeService = aiRuntimeService;
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
        draft.setStatus("OUTLINE_RUNNING");
        deckDraftRepository.save(draft);

        RenderedPrompt prompt = deckPromptService.outline(draft.getInitialPrompt());
        AiChatResponse response = aiRuntimeService.chat(
                LOCAL_USER_ID,
                prompt.messages(),
                prompt.responseFormat(),
                prompt.maxTokens()
        );
        DeckOutline outline = parseModelJson(response.content(), DeckOutline.class);
        List<SlideStickyNote> stickyNotes = toStickyNotes(outline);

        draft.setOutlineJson(toJson(outline));
        draft.setStickyNotesJson(toJson(stickyNotes));
        draft.setStatus("OUTLINE_READY");
        deckDraftRepository.save(draft);
        recordWorkflow(draft, prompt, toJson(outline), start);
        return outline;
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

    private void recordWorkflow(DeckDraftEntity draft, RenderedPrompt prompt, String outputJson, long start) {
        workflowRunRepository.save(new WorkflowRun(
                LOCAL_USER_ID,
                draft.getId(),
                "deckOutline",
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
                fromJson(draft.getOutlineJson(), DeckOutline.class),
                stickyNotesFromJson(draft.getStickyNotesJson())
        );
    }
}
