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
            PromptKeys.RESEARCH_COLLECT,
            new PromptTemplate(
                    PromptKeys.RESEARCH_COLLECT,
                    "v1",
                    COMMON_SYSTEM + """
                            你是 PPT 资料研究员。目标不是写长文，而是整理适合上屏的信息素材。
                            当前默认是 model-only 模式；没有真实来源时 sources 必须为空。
                            """,
                    """
                            请基于 brief 生成 model-only researchPack。
                            不要编造 URL 或外部来源，sources 必须为空数组。

                            brief JSON：
                            {{requirementBriefJson}}

                            JSON 字段必须完全如下：
                            {
                              "mode": "model-only",
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
                                {"id": "primary", "role": "primary", "type": "conclusion", "title": "string", "content": "string"}
                              ],
                              "layoutIntent": "string",
                              "visualStyle": "string"
                            }
                            """,
                    "json",
                    4096
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
                            请根据 pagePlan 生成一张 16:9、viewBox="0 0 1280 720" 的单页 PPT SVG。
                            严格要求：
                            - 只返回 <svg>...</svg>。
                            - 使用专业克制的 Bento Grid 布局。
                            - 文本不要重叠，所有元素必须在 viewBox 内。
                            - 背景、卡片、标题、正文必须有清晰层级。

                            pagePlan JSON：
                            {{pagePlanJson}}
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
