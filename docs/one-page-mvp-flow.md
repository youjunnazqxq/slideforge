# 一页 PPT 流程 MVP

本文档定义 SlideForge 第一阶段目标：先跑通“一页 PPT”的完整生成流程。

第一阶段不追求整套 PPT 自动生成，也不追求复杂模板库。目标是验证博主方案中最核心的链路是否成立：

```text
需求对话 -> 需求 brief -> 资料整理 -> 单页策划稿 -> Bento Grid 视觉方案 -> SVG 生成 -> 页面预览
```

## 1. 阶段目标

第一阶段只生成一页 PPT。

用户输入一个主题，系统通过 AI 对话澄清需求，然后生成这一页的内容策划稿，最后生成一张 16:9 的 SVG 页面。

成功标准：

- 用户可以和 AI 对话完成需求调研。
- 系统可以从对话中提取结构化需求 brief。
- 系统可以围绕主题搜索或整理资料。
- 系统可以生成单页 PPT 的策划稿。
- 系统可以生成 Bento Grid 风格的 SVG。
- 用户可以在网页中预览 SVG。
- 用户可以重新生成策划稿或 SVG。

## 2. 暂不做的内容

为了降低复杂度，第一阶段暂不做：

- 整套 PPT 批量生成。
- 多页便利贴排序。
- 完整 PPTX 导出。
- 模板市场。
- 多人协作。
- 复杂品牌规范。
- 版本历史。
- 用户账户体系。
- 复杂任务队列。

这些能力可以在一页闭环稳定后再逐步加入。

## 3. 推荐产品流程

### 3.1 用户输入初始想法

用户进入工作台后，先输入一句自然语言需求。

示例：

```text
我想做一页关于 AI PPT Agent 项目可行性的汇报页，给团队内部讨论用。
```

这一步只要求用户说清楚大概方向，不要求填写完整表单。

### 3.2 AI 需求顾问追问

AI 以顾问身份继续追问。

重点追问内容：

- 这页给谁看？
- 使用场景是什么？
- 这一页最想传达什么结论？
- 是否需要偏商业、技术、产品还是汇报风格？
- 有没有必须包含的信息？
- 有没有需要避免的表达？

为了避免无限对话，第一阶段设置简单规则：

- 最多追问 3 轮。
- 或用户点击“直接生成 brief”。
- 或 AI 判断信息已经足够。

### 3.3 生成结构化需求 brief

AI 从对话中提取结构化需求。

建议字段：

```json
{
  "topic": "AI PPT Agent 项目可行性",
  "audience": "团队内部成员",
  "scenario": "项目立项讨论",
  "goal": "帮助团队判断是否值得投入 MVP 开发",
  "pageCount": 1,
  "tone": "专业、务实、信息密度适中",
  "mustInclude": [
    "技术可行性",
    "产品价值",
    "主要风险",
    "下一步建议"
  ],
  "avoid": [
    "夸大商业价值",
    "过度承诺自动生成质量"
  ]
}
```

前端应允许用户手动编辑 brief。确认后进入下一步。

### 3.4 资料整理

第一阶段可以先做两种模式：

1. 无联网模式：AI 根据已有知识和用户输入整理资料。
2. 联网模式：后端调用搜索工具或模型自带搜索能力，整理来源信息。

资料整理输出不需要很长，重点是为一页 PPT 提供足够素材。

建议结构：

```json
{
  "summary": "围绕主题整理出的核心背景",
  "keyPoints": [
    "工作流型 AI 比一键生成更适合复杂 PPT 场景",
    "一页 MVP 可以先验证需求对话、策划稿和 SVG 生成质量",
    "主要风险在 SVG 稳定性、资料可靠性和用户编辑体验"
  ],
  "evidence": [
    {
      "claim": "分阶段生成可以提升可控性",
      "support": "用户能在 brief、策划稿和 SVG 阶段介入修改"
    }
  ],
  "sources": []
}
```

### 3.5 生成单页策划稿

策划稿是本项目的关键中间产物。它不是最终视觉页面，而是给 SVG 生成阶段的施工说明。

建议结构：

```json
{
  "slideTitle": "AI PPT Agent：先验证一页闭环的可行性",
  "coreMessage": "项目技术上可行，但第一阶段应聚焦一页流程，先验证工作流质量。",
  "audienceTakeaway": "团队应先投入 MVP，而不是一开始做完整 PPT SaaS。",
  "contentBlocks": [
    {
      "role": "primary",
      "type": "conclusion",
      "title": "核心判断",
      "content": "一页闭环可行性高，适合作为第一阶段目标。"
    },
    {
      "role": "supporting",
      "type": "list",
      "title": "要验证的能力",
      "content": "需求对话、结构化 brief、策划稿、SVG 生成、预览重试。"
    },
    {
      "role": "risk",
      "type": "list",
      "title": "主要风险",
      "content": "生成质量不稳定、SVG 易重叠、资料来源不充分。"
    },
    {
      "role": "next_step",
      "type": "recommendation",
      "title": "下一步",
      "content": "先实现单页 MVP，再扩展多页便利贴和 PPTX 导出。"
    }
  ],
  "layoutIntent": "使用 Bento Grid：左侧大卡片放核心判断，右侧三个纵向小卡片放验证能力、风险和下一步。",
  "visualStyle": "专业、克制、现代，避免花哨装饰，强调层级和留白。"
}
```

### 3.6 生成 Bento Grid 视觉方案

视觉方案将策划稿转成更接近页面结构的描述。

建议字段：

```json
{
  "canvas": {
    "width": 1280,
    "height": 720,
    "ratio": "16:9"
  },
  "theme": {
    "background": "#F7F8FA",
    "primary": "#2563EB",
    "text": "#111827",
    "muted": "#6B7280"
  },
  "layout": [
    {
      "id": "hero",
      "x": 64,
      "y": 96,
      "w": 560,
      "h": 480,
      "priority": "primary"
    },
    {
      "id": "capabilities",
      "x": 656,
      "y": 96,
      "w": 560,
      "h": 145,
      "priority": "supporting"
    }
  ]
}
```

第一阶段可以让 AI 直接从策划稿生成 SVG，不必单独展示视觉方案；但后端最好保留这个中间结构，方便调试。

### 3.7 生成 SVG

SVG 要求：

- 固定画布：`1280 x 720`。
- 使用 16:9 比例。
- 文本不能溢出卡片。
- 元素不能重叠。
- 不使用外部图片链接。
- 不使用脚本。
- 不使用危险标签。
- 卡片圆角控制在适度范围。
- 风格专业，适合 PPT 汇报。

后端生成 SVG 后，需要做清洗和校验：

- 移除 `<script>`。
- 移除外部资源引用。
- 校验是否包含 `<svg>` 根节点。
- 可选：用 SVGO 压缩优化。

### 3.8 前端预览与重试

用户看到 SVG 预览后，可以：

- 返回修改 brief。
- 返回修改策划稿。
- 重新生成 SVG。
- 下载 SVG。

第一阶段可以暂不做 PPTX 导出，只提供 SVG 下载。

## 4. 页面结构建议

一页 MVP 可以先做成一个工作台页面。

```text
顶部：流程进度条

左侧：当前阶段操作区
  - AI 对话
  - brief 编辑
  - 策划稿编辑

右侧：实时结果区
  - 需求 brief
  - 资料摘要
  - SVG 预览
```

流程状态：

```text
1. 需求调研
2. 资料整理
3. 页面策划稿
4. SVG 生成
5. 预览下载
```

## 5. 后端接口草案

如果采用 Vue3 前端 + 独立后端，可以先设计这些 API：

```text
POST /api/chat/consult
POST /api/brief/extract
POST /api/research/collect
POST /api/page-plan/generate
POST /api/svg/generate
POST /api/svg/regenerate
GET  /api/projects/:id
```

第一阶段也可以先不做项目持久化，用单个 `draftId` 或内存状态跑通流程。

## 6. 数据模型草案

第一阶段最少需要保存：

```text
OnePageDraft
  - id
  - initialPrompt
  - conversationMessages
  - requirementBrief
  - researchPack
  - pagePlan
  - visualSpec
  - svgContent
  - createdAt
  - updatedAt
```

如果要支持后续扩展，建议即使 MVP 只做一页，也按“项目 -> 页面”的关系设计：

```text
Project
  - id
  - title
  - createdAt
  - updatedAt

Slide
  - id
  - projectId
  - title
  - requirementBrief
  - researchPack
  - pagePlan
  - visualSpec
  - svgContent
  - status
```

## 7. 第一阶段验收标准

完成后应该能演示如下流程：

```text
1. 用户输入一句 PPT 需求。
2. AI 追问 1 到 3 轮。
3. 用户点击生成 brief。
4. 系统展示可编辑 brief。
5. 用户确认后生成资料摘要。
6. 用户点击生成策划稿。
7. 系统展示可编辑策划稿。
8. 用户点击生成 SVG。
9. 页面右侧展示 16:9 SVG 预览。
10. 用户可以下载 SVG 或重新生成。
```

如果这条链路稳定，项目就具备继续扩展到多页 PPT 的基础。

## 8. 下一步开发建议

建议接下来按这个顺序开发：

1. 搭建 Vue3 + TypeScript 前端工作台。
2. 搭建 Node.js 后端 API。
3. 做 AI Provider Adapter，先支持 OpenAI-compatible API。
4. 实现需求对话接口。
5. 实现 brief 提取接口。
6. 实现单页策划稿生成接口。
7. 实现 SVG 生成与清洗接口。
8. 做前端 SVG 预览。

第一轮开发的最小闭环可以暂时跳过联网搜索，先用 AI 根据用户输入和对话内容整理资料。等一页闭环稳定后，再加入搜索能力。
