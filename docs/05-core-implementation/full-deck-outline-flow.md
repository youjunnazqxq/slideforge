# Full Deck Outline Flow

本文档定义如何把当前“一页 PPT MVP”扩展为网站方案中的完整 PPT 大纲与便利贴工作流。

## 1. 背景

网站方案不是直接生成页面，而是先模拟专业 PPT 团队的结构规划：

```text
需求调研 -> 资料检索 -> 结构大纲 -> 便利贴排布 -> 逐页策划稿 -> Bento Grid SVG
```

当前 SlideForge 只实现了单页：

```text
brief -> researchPack -> pagePlan -> SVG
```

缺少整套 PPT 的结构大纲和便利贴式页面管理。

## 2. 目标

新增 full deck 工作流：

```text
DeckDraft
  -> DeckOutline
  -> SlideStickyNotes
  -> selected slide PagePlan
  -> selected slide SVG
```

第一阶段仍可复用 OnePage 的 pagePlan/SVG 能力，先把多页策划骨架跑通。

## 3. 数据结构

### 3.1 DeckOutline

```json
{
  "title": "string",
  "audience": "string",
  "scenario": "string",
  "coreThesis": "string",
  "structure": [
    {
      "id": "section-1",
      "title": "string",
      "purpose": "string"
    }
  ],
  "slides": [
    {
      "id": "slide-001",
      "type": "cover | agenda | section | content | summary",
      "sectionId": "section-1",
      "title": "string",
      "message": "string",
      "purpose": "string"
    }
  ]
}
```

### 3.2 SlideStickyNote

```json
{
  "slideId": "slide-001",
  "order": 1,
  "sectionTitle": "string",
  "title": "string",
  "message": "string",
  "status": "planned | researched | planned_page | svg_ready",
  "tags": ["string"]
}
```

## 4. API 设计

```text
POST /api/decks
GET  /api/decks/{deckId}
POST /api/decks/{deckId}/outline
PUT  /api/decks/{deckId}/outline
POST /api/decks/{deckId}/sticky-notes
PUT  /api/decks/{deckId}/sticky-notes/reorder
POST /api/decks/{deckId}/slides/{slideId}/page-plan
POST /api/decks/{deckId}/slides/{slideId}/svg
```

MVP 可先实现：

```text
POST /api/decks
POST /api/decks/{deckId}/outline
GET  /api/decks/{deckId}
```

## 5. Prompt 要求

`deck.outline.v1` 必须体现网站提示词思想：

- 你是顶级 PPT 结构架构师。
- 不生成视觉页面。
- 先判断表达目标，再设计结构。
- 遵循结论先行、以上统下、分类清晰、逻辑递进。
- 每页只承载一个主要表达任务。
- 输出 JSON，不输出 Markdown。

## 6. 前端工作区

左侧从“单页缩略图”升级为便利贴列表：

```text
Deck title
  Slide 1 cover
  Slide 2 problem
  Slide 3 solution
  Slide 4 workflow
```

中间区域：

- Outline 视图：展示整套 PPT 结构。
- Sticky Notes 视图：展示页面卡片，可排序。
- Slide Workspace：复用一页 pagePlan/SVG 编辑。

右侧区域：

- AI 生成状态。
- 当前选中页信息。
- 资料来源和生成动作。

## 7. 验收标准

1. 用户输入主题后可以生成整套 PPT 结构大纲。
2. 大纲包含 deck title、audience、scenario、coreThesis、slides。
3. slides 不是普通目录，而是每页都有表达目标和核心 message。
4. 前端能展示大纲和页面列表。
5. 用户可以选择某一页继续生成 pagePlan/SVG。
6. 不破坏当前 One Page MVP。
