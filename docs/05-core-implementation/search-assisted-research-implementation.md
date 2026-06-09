# Search Assisted Research Implementation

本文档定义如何把当前 `model-only` 资料整理升级为 `search-assisted`，以贴近网站方案中“先检索资料，再生成大纲和页面”的要求。

## 1. 背景

当前 Research Agent 已生成结构化 `ResearchPack`，但 sources 为空。

这符合一页 MVP 的兜底模式，但还没有达到网站方案强调的：

- 先补充资料。
- 保留来源链接。
- 用资料支撑大纲和页面。
- 避免模型凭空编造事实。

## 2. 目标

新增两种资料模式：

```text
model-only
search-assisted
```

`model-only`：

- 不联网。
- sources 必须为空。
- limitations 必须说明没有外部来源。

`search-assisted`：

- 先根据 brief 生成 3-5 个搜索 query。
- 调用 SearchClient 获取搜索结果。
- 去重、过滤、截断。
- 将 sources 传给 Research Agent。
- evidence.sourceIds 必须引用已存在 sources。

## 3. 后端结构

```text
apps/api/src/main/java/com/slideforge/api/research/
  SearchClient.java
  SearchResult.java
  SearchQueryPlan.java
  SearchQueryPlanner.java
  SearchProviderProperties.java
  NoopSearchClient.java
  TavilySearchClient.java 或 SerpApiSearchClient.java
```

MVP 先实现 `NoopSearchClient`，保留接口。
后续根据用户选择接入 Tavily、SerpAPI、Brave Search 或模型自带联网能力。

## 4. API 设计

当前接口：

```text
POST /api/one-page/drafts/{draftId}/research
```

扩展请求体：

```json
{
  "mode": "model-only | search-assisted"
}
```

兼容策略：

- 无请求体时默认 `model-only`。
- 当前前端可先不传，后续加开关。

## 5. Search Query Plan

```json
{
  "queries": [
    {
      "query": "AI PPT Agent workflow Bento Grid SVG",
      "intent": "case"
    }
  ]
}
```

Intent 可选：

```text
definition
trend
data
case
risk
comparison
```

## 6. Search Result

```json
{
  "id": "src-001",
  "title": "string",
  "url": "https://example.com",
  "publisher": "string",
  "publishedAt": "string",
  "snippet": "string"
}
```

## 7. ResearchPack 生成规则

传给 Research Agent 的输入：

```text
requirementBriefJson
searchResultsJson
researchMode
```

约束：

- sources 只能来自 searchResults。
- 没有 source 支撑的内容不要伪装成事实。
- 如果搜索结果质量不足，写入 limitations。
- keyPoints 必须短句化，适合放入 PPT。

## 8. 前端设计

资料阶段增加 segmented control：

```text
[Model Only] [Search Assisted]
```

资料结果展示：

- mode。
- summary。
- keyPoints。
- evidence。
- sources。
- limitations。

当 `search-assisted` 不可用：

- 显示“当前未配置搜索服务，已回退到 model-only”。

## 9. 验收标准

1. model-only 模式 sources 必须为空。
2. search-assisted 模式能通过 SearchClient 抽象返回 sources。
3. 没有配置搜索服务时不会失败整条流程，可回退 model-only。
4. ResearchPack 的 sources 可以在前端展示。
5. 后续 deck outline 可以使用 researchPack 作为输入。
