# Deck Agent API Guide

本文档面向前端和外部集成方，说明如何通过 REST API 调用 SlideForge 的完整 PPT Agent 流程。

当前契约来源：`apps/api/src/main/java/com/slideforge/api` 下的 Controller 与 Service 代码。

## Base URL

```text
Local: http://localhost:8080
API prefix: /api
```

统一 JSON 响应：

```json
{
  "code": 0,
  "message": "OK",
  "data": {}
}
```

PPTX 导出接口返回二进制文件，不包裹在统一 JSON 响应中。

## 1. Configure BYOK AI Provider

SlideForge 使用用户自己的 OpenAI-compatible API 配置。没有可用配置时，Agent Flow 会在调用模型阶段失败。

### GET /api/settings/ai

读取当前 AI 配置。响应不会返回明文 API Key。

```bash
curl http://localhost:8080/api/settings/ai
```

响应：

```json
{
  "code": 0,
  "message": "OK",
  "data": {
    "provider": "openai-compatible",
    "baseUrl": "https://api.openai.com/v1",
    "apiKeyConfigured": true,
    "apiKeyMask": "sk-****abcd",
    "model": "gpt-4.1",
    "temperature": 0.7,
    "maxTokens": 4096
  }
}
```

### PUT /api/settings/ai

保存或更新 AI 配置。

```bash
curl -X PUT http://localhost:8080/api/settings/ai \
  -H "Content-Type: application/json" \
  -d '{
    "provider": "openai-compatible",
    "baseUrl": "https://api.openai.com/v1",
    "apiKey": "sk-...",
    "model": "gpt-4.1",
    "temperature": 0.7,
    "maxTokens": 4096
  }'
```

必填字段：

| Field | Required | Notes |
| --- | --- | --- |
| provider | yes | 当前实现使用 `openai-compatible` |
| baseUrl | yes | OpenAI-compatible `/v1` base URL |
| apiKey | no | 更新时为空会沿用已有 key |
| model | yes | 默认聊天模型 |
| temperature | no | 由服务端透传给 provider |
| maxTokens | no | 由服务端透传给 provider |

### POST /api/settings/ai/test

测试当前或临时 AI 配置是否可用。

```bash
curl -X POST http://localhost:8080/api/settings/ai/test \
  -H "Content-Type: application/json" \
  -d '{
    "provider": "openai-compatible",
    "baseUrl": "https://api.openai.com/v1",
    "apiKey": "sk-...",
    "model": "gpt-4.1"
  }'
```

## 2. Create Deck Draft

### POST /api/decks

创建整套 PPT 草稿。

```bash
curl -X POST http://localhost:8080/api/decks \
  -H "Content-Type: application/json" \
  -d '{
    "initialPrompt": "我想做一套关于 AI PPT Agent 项目可行性的内部立项汇报，目标是说服团队投入 MVP。"
  }'
```

响应：

```json
{
  "code": 0,
  "message": "OK",
  "data": {
    "deckId": "f2d2b2f3-0000-0000-0000-000000000000",
    "status": "CREATED"
  }
}
```

## 3. Run Full Agent Flow

### POST /api/decks/{deckId}/agent-flow

按顺序执行：

`consult -> research -> outline -> sticky notes -> one-page drafts -> page plans -> Bento specs -> SVG -> failed SVG retry`

```bash
curl -X POST http://localhost:8080/api/decks/f2d2b2f3-0000-0000-0000-000000000000/agent-flow \
  -H "Content-Type: application/json" \
  -d '{
    "mode": "model-only"
  }'
```

`mode` 可选值：

| Value | Behavior |
| --- | --- |
| model-only | 只使用模型整理资料，不编造外部 URL |
| search-assisted | 优先使用搜索结果；如果没有搜索结果，会回退到 model-only |

响应为完整 Deck 状态：

```json
{
  "code": 0,
  "message": "OK",
  "data": {
    "deckId": "f2d2b2f3-0000-0000-0000-000000000000",
    "status": "SLIDE_SVGS_READY",
    "initialPrompt": "...",
    "researchPack": {},
    "outline": {},
    "stickyNotes": [],
    "generatedDrafts": [
      {
        "slideId": "slide-001",
        "order": 1,
        "title": "AI PPT Agent 项目可行性汇报",
        "draftId": "uuid",
        "status": "SVG_READY",
        "errorMessage": null
      }
    ]
  }
}
```

## 4. Rerun Individual Steps

这些接口用于前端时间线上的单步重跑。

| Step | Endpoint | Notes |
| --- | --- | --- |
| consult | `POST /api/decks/{deckId}/consult` | 请求体 `{ "message": "..." }` |
| research | `POST /api/decks/{deckId}/research` | 请求体 `{ "mode": "model-only" }` |
| outline | `POST /api/decks/{deckId}/outline` | 会确保 research pack 存在 |
| one-page drafts | `POST /api/decks/{deckId}/slides/one-page-drafts` | 复用已有 draftId，避免覆盖用户编辑 |
| page plans | `POST /api/decks/{deckId}/slides/page-plan-drafts` | 为每页生成或刷新 pagePlan |
| Bento specs | `POST /api/decks/{deckId}/slides/visual-spec-drafts` | 为每页生成或刷新 visualSpec |
| SVG pages | `POST /api/decks/{deckId}/slides/svg-drafts` | 为每页生成 SVG |
| single SVG retry | `POST /api/decks/{deckId}/slides/{slideId}/svg-draft` | 只重试一页 |

## 5. Export PPTX

Deck 导出复用 one-page draft 批量导出接口。

前置条件：

- `generatedDrafts` 中每一页都必须有 `draftId`。
- 每一页状态必须是 `SVG_READY`。
- 后端会拒绝状态不是 `SVG_READY` 的页面。

请求：

```bash
curl -X POST http://localhost:8080/api/one-page/drafts/export/pptx \
  -H "Content-Type: application/json" \
  -o slideforge-deck.pptx \
  -d '{
    "draftIds": [
      "11111111-1111-1111-1111-111111111111",
      "22222222-2222-2222-2222-222222222222"
    ]
  }'
```

成功响应：

```text
Content-Type: application/vnd.openxmlformats-officedocument.presentationml.presentation
Content-Disposition: attachment; filename="slideforge-deck-2-pages.pptx"
```

## 6. Common Failures

| Case | HTTP behavior | Fix |
| --- | --- | --- |
| AI provider missing or invalid | Model-calling endpoint fails | Configure `/api/settings/ai`, then run `/test` |
| Search-assisted mode has no sources | Falls back to model-only | Check search provider configuration if search is expected |
| Draft does not exist | 404 | Recreate deck or refresh saved `deckId` |
| JSON returned by model is invalid | 502 | Rerun the step; JSON repair is attempted where implemented |
| SVG not validation-ready | 400 on PPTX export | Rerun SVG or manually edit SVG until status is `SVG_READY` |
| Some deck pages failed | Deck status may be partial | Retry failed page SVG or rerun the blocked step |

## 7. Drift Control

Refresh this document when any of these files change:

- `DeckDraftController.java`
- `DeckDraftService.java`
- `OnePageDraftController.java`
- `OnePagePptxExportService.java`
- `AiSettingsController.java`
- DTOs under `apps/api/src/main/java/com/slideforge/api/deck/dto`
- DTOs under `apps/api/src/main/java/com/slideforge/api/settings/dto`
