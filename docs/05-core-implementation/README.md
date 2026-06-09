# 05 Core Implementation

本目录记录 SlideForge 从“框架已搭好”走向“真实一页 PPT 生成闭环”所需的核心功能实施流程。

当前已完成的是前后端大体框架：

```text
前端页面骨架
后端接口骨架
AI Provider Adapter 占位
One Page Draft 内存流程
SVG 示例生成与校验
```

剩余核心工作是把这些占位能力替换为真实能力。

## 文档列表

| 文档 | 说明 |
| --- | --- |
| `core-roadmap.md` | 剩余核心功能的总体实施顺序。 |
| `frontend-backend-integration.md` | 前端一页工作台如何逐步接入后端接口。 |
| `real-ai-provider-flow.md` | 用户 API Key 如何驱动真实 AI 模型调用。 |
| `prompt-runtime-flow.md` | prompt 模板如何在后端运行、校验和修复输出。 |
| `persistence-flow.md` | AI 设置、草稿、中间产物和 WorkflowRun 如何持久化。 |
| `api-key-security-flow.md` | API Key 加密、掩码、删除和日志脱敏流程。 |
| `research-search-flow.md` | 资料检索、来源保存和 researchPack 生成流程。 |
| `svg-quality-flow.md` | SVG 生成质量、清洗、校验、重试和前端预览流程。 |
| `pptx-export-flow.md` | 后续 PPTX 导出的实施路线。 |
| `prompt-library-implementation.md` | 将网站提示词方法落成后端 Prompt Library / Runtime 的实施方案。 |
| `full-deck-outline-flow.md` | 从一页 MVP 扩展到完整 PPT 大纲和便利贴工作流的实施方案。 |
| `search-assisted-research-implementation.md` | 将 model-only 资料整理升级为联网资料检索的实施方案。 |

## 推荐开发顺序

1. `frontend-backend-integration.md`
2. `persistence-flow.md`
3. `api-key-security-flow.md`
4. `real-ai-provider-flow.md`
5. `prompt-runtime-flow.md`
6. `svg-quality-flow.md`
7. `research-search-flow.md`
8. `pptx-export-flow.md`
