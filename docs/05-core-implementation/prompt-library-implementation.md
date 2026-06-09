# Prompt Library Implementation

本文档定义如何把网站方案中的核心提示词方法，落成 SlideForge 后端可版本化、可追踪、可迭代的 Prompt Runtime。

## 1. 背景

当前后端已经能调用用户配置的 OpenAI-compatible API，并完成一页 PPT 的：

```text
consult -> brief -> researchPack -> pagePlan -> SVG -> PPTX
```

但提示词仍写在 Java Service 里，缺少：

- promptKey 与版本管理。
- system/user message 分层。
- 网站方案中的完整角色分工。
- JSON/SVG repair prompt。
- 统一的渲染、记录和审计。

## 2. 目标

第一阶段把提示词从业务 Service 中抽出，形成内置 Prompt Library。

目标能力：

```text
PromptTemplateRegistry
  -> 根据 promptKey 取模板
PromptRenderer
  -> 将变量渲染为 user message
OnePagePromptService
  -> 组合各阶段 messages
WorkflowRun
  -> 记录 promptKey / promptVersion / model / output
```

## 3. Prompt Key

| Key | 角色 | 输出 |
| --- | --- | --- |
| `consultant.v1` | PPT 需求顾问 | 自然语言追问或确认 |
| `brief.extract.v1` | Brief Agent | `RequirementBrief` JSON |
| `research.collect.v1` | Research Agent | `ResearchPack` JSON |
| `deck.outline.v1` | 顶级 PPT 结构架构师 | `DeckOutline` JSON |
| `sticky-notes.v1` | Storyboard Agent | `SlideStickyNote[]` JSON |
| `page-plan.generate.v1` | Page Planner Agent | `PagePlan` JSON |
| `visual-spec.generate.v1` | Visual Designer Agent | `VisualSpec` JSON |
| `svg.generate.v1` | SVG Engineer Agent | 完整 SVG |
| `json.repair.v1` | JSON Repair Agent | 修复后的 JSON |
| `svg.repair.v1` | SVG Repair Agent | 修复后的 SVG |

## 4. 网站提示词映射

网站强调的“顶级 PPT 结构架构师”应映射为 `deck.outline.v1`。

核心要求：

- 结论先行。
- 以上统下。
- 逻辑递进。
- 每页只承载一个主要表达任务。
- 输出结构化大纲，而不是普通目录。

网站强调的 Bento Grid SVG 设计提示词应拆成两层：

- `visual-spec.generate.v1`：决定卡片数量、坐标、层级、主题。
- `svg.generate.v1`：根据 pagePlan + visualSpec 生成安全 SVG。

这样可以降低模型直接写 SVG 时的随机性。

## 5. 后端结构

建议新增：

```text
apps/api/src/main/java/com/slideforge/api/ai/prompt/
  PromptTemplate.java
  PromptTemplateRegistry.java
  PromptRenderer.java
  PromptKeys.java
  RenderedPrompt.java

apps/api/src/main/java/com/slideforge/api/onepage/
  OnePagePromptService.java
```

## 6. Template 结构

```java
record PromptTemplate(
  String key,
  String version,
  String systemPrompt,
  String userTemplate,
  String responseFormat,
  Integer maxTokens
) {}
```

`userTemplate` 使用简单变量占位：

```text
{{initialPrompt}}
{{requirementBriefJson}}
{{researchPackJson}}
{{pagePlanJson}}
{{visualSpecJson}}
```

## 7. 渲染输出

```java
record RenderedPrompt(
  String key,
  String version,
  List<AiMessage> messages,
  String responseFormat,
  Integer maxTokens
) {}
```

## 8. JSON Repair 流程

适用阶段：

- brief。
- research。
- deck outline。
- sticky notes。
- page plan。
- visual spec。

流程：

```text
call target prompt
  -> extract JSON
  -> ObjectMapper.readValue
  -> success: save
  -> failed: call json.repair.v1 once
  -> parse again
  -> failed: return clear error
```

MVP 可以先只抽离模板，repair 作为后续实现。

## 9. SVG Repair 流程

```text
call svg.generate.v1
  -> extract <svg>
  -> sanitize
  -> validate
  -> valid: save
  -> invalid: call svg.repair.v1 once
  -> sanitize / validate again
```

MVP 可以先保留当前 sanitize/validate，再补 repair。

## 10. 验收标准

1. 后端 Service 中不再硬编码长提示词。
2. 每次 WorkflowRun 有明确 promptKey 和 promptVersion。
3. prompt 模板可以在一个类或目录中集中维护。
4. brief/research/pagePlan/SVG 的生成结果与现有接口兼容。
5. 后续新增 full deck outline 不需要复制粘贴大段 prompt 到 Controller 或 Service。
