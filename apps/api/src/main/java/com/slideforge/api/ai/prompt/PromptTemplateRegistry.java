package com.slideforge.api.ai.prompt;

import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
public class PromptTemplateRegistry {

    private static final String COMMON_SYSTEM = """
            你是 SlideForge 的 AI PPT 工作流组件。你的输出必须服务于专业 PPT 制作。
            不要生成营销式废话。不要编造来源、数据、机构或链接。
            如果要求 JSON，只输出 JSON，不要输出 Markdown。
            如果要求 SVG，只输出完整 SVG，不要输出解释文字。
            """;

    private final Map<String, PromptTemplate> templates = Map.of(
            PromptKeys.CONSULTANT,
            new PromptTemplate(
                    PromptKeys.CONSULTANT,
                    "v1",
                    COMMON_SYSTEM + """
                            你是专业 PPT 需求顾问。你的任务不是直接生成 PPT，而是澄清用户目标、观众、场景、核心结论、必须包含和需要避免的信息。
                            每轮最多提出 5 个具体问题；如果信息足够，请明确告诉用户可以生成 brief。
                            """,
                    """
                            用户初始需求：
                            {{initialPrompt}}

                            用户补充：
                            {{message}}

                            请继续追问，或判断是否可以生成 brief。输出 2-4 句中文。
                            """,
                    "text",
                    1024
            ),
            PromptKeys.BRIEF_EXTRACT,
            new PromptTemplate(
                    PromptKeys.BRIEF_EXTRACT,
                    "v1",
                    COMMON_SYSTEM + """
                            你是 PPT 需求分析师，负责从用户输入中提取结构化 brief。
                            如果某个字段无法确定，使用空字符串或空数组。不要补充用户没有表达过的具体事实。
                            """,
                    """
                            请根据以下用户需求提取一页 PPT 的需求 brief。

                            用户需求：
                            {{initialPrompt}}

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
                            """,
                    "json",
                    2048
            ),
            PromptKeys.SEARCH_QUERY_PLAN,
            new PromptTemplate(
                    PromptKeys.SEARCH_QUERY_PLAN,
                    "v1",
                    COMMON_SYSTEM + """
                            你是 PPT 资料检索策略师。你的任务是先规划搜索词，而不是直接写内容。
                            搜索词要服务于 PPT 决策，不追求数量，优先覆盖行业事实、竞品/方法、风险和关键概念。
                            """,
                    """
                            请基于 brief 生成 3-5 条适合联网检索的 query。
                            要求：
                            - query 必须具体，避免空泛词。
                            - 如果 brief 是中文，query 可以中英混合，优先选择更可能搜到高质量资料的表达。
                            - 不要编造来源，不要输出 Markdown。

                            brief JSON：
                            {{requirementBriefJson}}

                            JSON 字段必须完全如下：
                            {
                              "queries": ["string"]
                            }
                            """,
                    "json",
                    1024
            ),
            PromptKeys.RESEARCH_COLLECT,
            new PromptTemplate(
                    PromptKeys.RESEARCH_COLLECT,
                    "v1",
                    COMMON_SYSTEM + """
                            你是 PPT 资料研究员。目标不是写长文，而是整理适合上屏的信息素材。
                            当前默认是 model-only 模式；没有真实来源时 sources 必须为空。
                            """,
                    """
                            请基于 brief 生成 researchPack。
                            researchMode={{researchMode}}
                            如果 researchMode 是 model-only，不要编造 URL 或外部来源，sources 必须为空数组。
                            如果 researchMode 是 search-assisted，sources 只能来自给定 sourcesJson。

                            brief JSON：
                            {{requirementBriefJson}}

                            sourcesJson：
                            {{sourcesJson}}

                            JSON 字段必须完全如下：
                            {
                              "mode": "{{researchMode}}",
                              "summary": "string",
                              "keyPoints": ["string"],
                              "evidence": [{"claim": "string", "support": "string", "sourceIds": []}],
                              "sources": [],
                              "limitations": ["string"]
                            }
                            """,
                    "json",
                    3072
            ),
            PromptKeys.PAGE_PLAN_GENERATE,
            new PromptTemplate(
                    PromptKeys.PAGE_PLAN_GENERATE,
                    "v1",
                    COMMON_SYSTEM + """
                            你是资深 PPT 页面策划师。策划稿不是最终页面，而是给视觉设计和 SVG 生成阶段的施工说明。
                            每页只表达一个核心观点，明确主信息和辅助信息，内容适合 16:9 页面。
                            """,
                    """
                            请基于 brief 和 researchPack 生成一页 16:9 PPT 的 pagePlan。
                            内容应适合 Bento Grid：一个核心结论块，2-4 个支撑块。
                            每个 contentBlocks.id 必须稳定、简短、可作为后续 visualSpec.cards.blockId 引用。
                            不要写长段落；每个 content 控制在 45 个汉字或 22 个英文词以内，适合直接上屏。
                            如果 researchPack.limitations 存在，请把不确定性转化为风险/假设类辅助块，而不是编造结论。
                            如果内容块来自 researchPack.evidence 或 sources，请填写 sourceIds；没有来源则使用空数组。

                            brief JSON：
                            {{requirementBriefJson}}

                            researchPack JSON：
                            {{researchPackJson}}

                            JSON 字段必须完全如下：
                            {
                              "slideTitle": "string",
                              "coreMessage": "string",
                              "audienceTakeaway": "string",
                              "contentBlocks": [
                                {"id": "primary", "role": "primary", "type": "conclusion", "title": "string", "content": "string", "sourceIds": []}
                              ],
                              "speakerIntent": "string",
                              "layoutIntent": "string",
                              "visualStyle": "string"
                            }
                            """,
                    "json",
                    4096
            ),
            PromptKeys.VISUAL_SPEC_GENERATE,
            new PromptTemplate(
                    PromptKeys.VISUAL_SPEC_GENERATE,
                    "v1",
                    COMMON_SYSTEM + """
                            你是顶级 PPT 视觉设计师，负责把页面策划稿转换为 Bento Grid 视觉规格。
                            你只设计画布、主题色和卡片排布，不生成 SVG 代码。
                            视觉规格必须服务内容层级：primary 内容最大，supporting/risk/next 内容较小，所有卡片必须在 1280x720 内。
                            """,
                    """
                            请基于 pagePlan 生成一份 16:9 Bento Grid visualSpec。
                            要求：
                            - canvas 固定为 width=1280, height=720, viewBox="0 0 1280 720"。
                            - cards 必须覆盖 pagePlan.contentBlocks 中的主要 block id。
                            - 卡片坐标和尺寸使用整数，x/y/w/h 不能超出画布。
                            - 任意两张卡片之间至少保留 20px 间距。
                            - cards 数量控制在 3-6 个；primary 卡片面积最大，supporting 卡片要围绕 primary 排布。
                            - 每个 card.id 必须稳定，后续 SVG 元素应使用 data-card-id 绑定。
                            - 选择一个 layoutPattern，并让坐标真实匹配该模式：
                              1. hero-left: 左侧大主卡，右侧 2-4 张纵向辅助卡。
                              2. hero-top: 顶部横向主卡，下方 3 张辅助卡。
                              3. mosaic: 中央主卡，四周小卡环绕。
                              4. split-hero: 左右双栏，主卡占其中一栏的大部分面积。
                            - 不要生成平均网格；Bento 必须有明显的视觉重心。
                            - 使用专业克制的非单色主题，避免整页只有一种蓝/紫/灰。

                            pagePlan JSON：
                            {{pagePlanJson}}

                            JSON 字段必须完全如下：
                            {
                              "canvas": {"width": 1280, "height": 720, "viewBox": "0 0 1280 720"},
                              "theme": {
                                "background": "#F7F8FA",
                                "primary": "#2563EB",
                                "text": "#111827",
                                "muted": "#6B7280",
                                "card": "#FFFFFF",
                                "border": "#E5E7EB"
                              },
                              "layoutPattern": "hero-left | hero-top | mosaic | split-hero",
                              "cards": [
                                {"id": "hero", "blockId": "primary", "x": 64, "y": 96, "w": 560, "h": 520, "priority": "primary"}
                              ]
                            }
                            """,
                    "json",
                    3072
            ),
            PromptKeys.SVG_GENERATE,
            new PromptTemplate(
                    PromptKeys.SVG_GENERATE,
                    "v1",
                    COMMON_SYSTEM + """
                            你是专业 SVG 信息设计师，擅长把结构化 PPT 策划稿转换为高质量 Bento Grid SVG。
                            严禁 script、foreignObject、外链图片、外链字体和外部 CSS。
                            """,
                    """
                            请根据 pagePlan 和 visualSpec 生成一张 16:9、viewBox="0 0 1280 720" 的单页 PPT SVG。
                            严格要求：
                            - 只返回 <svg>...</svg>。
                            - svg 根节点必须包含 xmlns="http://www.w3.org/2000/svg"、width="1280"、height="720"、viewBox="0 0 1280 720"。
                            - 使用专业克制的 Bento Grid 布局，并严格遵守 visualSpec.cards 的 x/y/w/h。
                            - 每个主要卡片 group 使用 <g data-card-id="..." data-block-id="...">，便于后续编辑。
                            - 文本不要重叠，所有元素必须在 viewBox 内。
                            - 背景、卡片、标题、正文必须有清晰层级。
                            - 不要使用 <style>、filter、clipPath、mask、foreignObject、script、image、base64、data URL、外链资源。
                            - 文字使用 system-ui, Arial, "Microsoft YaHei", sans-serif；长文本拆成多行 <text> 或 <tspan>。

                            pagePlan JSON：
                            {{pagePlanJson}}

                            visualSpec JSON：
                            {{visualSpecJson}}
                            """,
                    "text",
                    4096
            ),
            PromptKeys.DECK_OUTLINE,
            new PromptTemplate(
                    PromptKeys.DECK_OUTLINE,
                    "v1",
                    COMMON_SYSTEM + """
                            你是顶级 PPT 结构架构师。你的任务是设计整套 PPT 的表达结构，不生成页面视觉。
                            必须遵循结论先行、以上统下、分类清晰、逻辑递进。每一页只承载一个主要表达任务。
                            输出必须像专业咨询或商业汇报大纲，而不是普通目录。
                            """,
                    """
                            请根据用户需求生成完整 PPT 大纲。
                            大纲必须保留“便利贴可编辑性”：slides 中每一页都要能独立移动、删除或改写，不依赖上一页隐藏上下文。
                            避免一页塞多个任务；如果一个页面同时想讲背景、证据、方案和风险，请拆成多页。
                            slides.title 要像真实 PPT 页标题，slides.message 要像这一页观众应该记住的一句话。

                            用户需求：
                            {{initialPrompt}}

                            JSON 字段必须完全如下：
                            {
                              "title": "string",
                              "audience": "string",
                              "scenario": "string",
                              "coreThesis": "string",
                              "structure": [
                                {"id": "section-1", "title": "string", "purpose": "string"}
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
                            """,
                    "json",
                    4096
            ),
            PromptKeys.JSON_REPAIR,
            new PromptTemplate(
                    PromptKeys.JSON_REPAIR,
                    "v1",
                    COMMON_SYSTEM + """
                            你是 JSON 修复器。你的任务是把输入内容修复成合法 JSON。
                            不要改变字段含义，不要新增业务内容，不要输出 Markdown。
                            """,
                    """
                            请修复以下内容，使其成为可被 JSON.parse / ObjectMapper 解析的 JSON。

                            原始内容：
                            {{brokenContent}}
                            """,
                    "json",
                    2048
            ),
            PromptKeys.SVG_REPAIR,
            new PromptTemplate(
                    PromptKeys.SVG_REPAIR,
                    "v1",
                    COMMON_SYSTEM + """
                            你是 SVG 修复器。请修复输入 SVG，使其满足安全和渲染要求。
                            必须移除 script、foreignObject、外部图片、外部字体和外部 CSS。
                            根节点必须是 svg，viewBox 必须是 0 0 1280 720。
                            输出必须包含 width="1280"、height="720" 和闭合 </svg>。
                            不要使用 style/filter/clipPath/mask/image/base64/data URL/外链资源。
                            """,
                    """
                            请修复以下 SVG。只输出完整 <svg>...</svg>。

                            SVG：
                            {{brokenContent}}
                            """,
                    "text",
                    4096
            )
    );

    public PromptTemplate get(String key) {
        PromptTemplate template = templates.get(key);

        if (template == null) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Prompt template not found: " + key);
        }

        return template;
    }
}
