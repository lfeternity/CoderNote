package com.codernote.platform.service.impl;

import com.codernote.platform.dto.ai.AiChatRequest;
import com.codernote.platform.dto.ai.AiChatVO;
import com.codernote.platform.dto.ai.AiModelConfig;
import com.codernote.platform.dto.ai.AiModelCatalogVO;
import com.codernote.platform.dto.ai.AiQuestionAnalysisRequest;
import com.codernote.platform.dto.ai.AiQuestionAnalysisVO;
import com.codernote.platform.dto.ai.AiSummaryRequest;
import com.codernote.platform.dto.ai.AiSummaryVO;
import com.codernote.platform.config.AiProviderProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.codernote.platform.dto.material.MaterialDetailVO;
import com.codernote.platform.dto.note.NoteDetailVO;
import com.codernote.platform.dto.question.QuestionDetailVO;
import com.codernote.platform.exception.BizException;
import com.codernote.platform.service.AiAssistantService;
import com.codernote.platform.service.MaterialService;
import com.codernote.platform.service.NoteService;
import com.codernote.platform.service.QuestionService;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class AiAssistantServiceImpl implements AiAssistantService {

    private static final int FREE_DAILY_LIMIT = 10;
    private static final ZoneId SHANGHAI_ZONE = ZoneId.of("Asia/Shanghai");

    private static final String DEFAULT_MODEL = "SAFE_GPT_SIM";
    private static final String MODEL_QWEN = "QWEN";
    private static final String MODEL_KIMI = "KIMI";
    private static final String MODEL_OPENAI = "OPENAI";

    private static final Set<String> SUPPORTED_MODELS = Set.of(
            DEFAULT_MODEL,
            MODEL_QWEN,
            MODEL_KIMI,
            MODEL_OPENAI
    );

    private static final String SUMMARY_CORE_EXTRACT = "CORE_EXTRACT";
    private static final String SUMMARY_MIND_MAP = "MIND_MAP";
    private static final String SUMMARY_INTERVIEW = "INTERVIEW";
    private static final String SUMMARY_FLASH_CARD = "FLASH_CARD";

    private static final Set<String> SUPPORTED_MATERIAL_SUMMARY_TYPES = Set.of("KNOWLEDGE_NOTE", "SOLUTION_TUTORIAL");

    private static final List<String> BLOCKED_TERMS = Arrays.asList(
            "木马", "勒索", "政治敏感", "sql注入", "drop table", "rm -rf", "shellcode", "撞库", "爆破"
    );

    private static final Pattern EXCEPTION_TYPE_PATTERN = Pattern.compile("([A-Za-z0-9_$.]+(?:Exception|Error))");
    private static final Pattern STACK_LINE_PATTERN = Pattern.compile(":(\\d+)");

    private final QuestionService questionService;
    private final NoteService noteService;
    private final MaterialService materialService;
    private final AiProviderProperties aiProviderProperties;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    private final Map<Long, DailyUsageRecord> usageByUser = new ConcurrentHashMap<>();

    public AiAssistantServiceImpl(QuestionService questionService,
                                  NoteService noteService,
                                  MaterialService materialService,
                                  AiProviderProperties aiProviderProperties,
                                  ObjectMapper objectMapper) {
        this.questionService = questionService;
        this.noteService = noteService;
        this.materialService = materialService;
        this.aiProviderProperties = aiProviderProperties;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    @Override
    public AiModelCatalogVO models() {
        String configuredDefault = normalizeModel(aiProviderProperties.getDefaultModel());

        AiModelCatalogVO vo = new AiModelCatalogVO();
        vo.setDefaultModel(configuredDefault);

        List<AiModelCatalogVO.ModelOption> list = new ArrayList<>();
        list.add(model(DEFAULT_MODEL, "安全增强模型", "本地安全策略生成，稳定可用"));
        list.add(model(
                MODEL_QWEN,
                "Qwen",
                hasProviderKey(MODEL_QWEN)
                        ? "已配置 API Key，将通过 Qwen 接口调用"
                        : "未配置 API Key，配置后可调用"
        ));
        list.add(model(
                MODEL_KIMI,
                "Kimi",
                hasProviderKey(MODEL_KIMI)
                        ? "已配置 API Key，将通过 Kimi 接口调用"
                        : "未配置 API Key，配置后可调用"
        ));
        list.add(model(
                MODEL_OPENAI,
                "OpenAI",
                hasProviderKey(MODEL_OPENAI)
                        ? "已配置 API Key，将通过 OpenAI 接口调用"
                        : "未配置 API Key，配置后可调用"
        ));
        vo.setModels(list);
        return vo;
    }

    @Override
    public AiQuestionAnalysisVO analyzeQuestion(Long userId, AiQuestionAnalysisRequest request) {
        QuestionContext context = resolveQuestionContext(userId, request);
        if (!StringUtils.hasText(context.errorCode)
                && !StringUtils.hasText(context.exceptionMessage)
                && !StringUtils.hasText(context.contextDescription)) {
            throw new BizException(400, "请至少提供错误代码、异常信息或上下文描述");
        }
        guardUnsafe(context.errorCode, context.exceptionMessage, context.contextDescription);
        Usage usage = consumeQuota(userId);

        String exceptionType = detectExceptionType(context.exceptionMessage, context.errorCode);
        String location = detectLocation(context.exceptionMessage, context.errorCode);
        String coreReason = inferReason(exceptionType, context.contextDescription, context.tagNames);
        String principle = inferPrinciple(exceptionType, context.language);
        String fixedCode = buildFixCode(context.language, context.errorCode, context.tagNames);
        String solutionText = "先定位触发条件，再补空值与边界保护，最后补回归用例防止同类问题复发。";

        AiQuestionAnalysisVO vo = new AiQuestionAnalysisVO();
        vo.setModel(context.model);
        vo.setUsedCount(usage.used);
        vo.setRemainingCount(usage.remaining);
        vo.setExceptionType(exceptionType);
        vo.setErrorLocation(location);
        vo.setCoreReason(coreReason);
        vo.setPrinciple(principle);
        vo.setFixedCode(fixedCode);
        vo.setSolutionText(solutionText);
        vo.setRelatedSuggestions(buildSuggestions(context.tagNames, context.language));

        String markdown = buildAnalysisMarkdown(context, exceptionType, location, coreReason, principle, fixedCode, vo.getRelatedSuggestions());
        if (isProviderModel(context.model)) {
            String providerMarkdown = callProviderForAnalysis(context, markdown);
            if (StringUtils.hasText(providerMarkdown)) {
                markdown = providerMarkdown;
            }
        }
        vo.setMarkdown(markdown);
        return vo;
    }

    @Override
    public AiSummaryVO summarize(Long userId, AiSummaryRequest request) {
        SummaryContext context = resolveSummaryContext(userId, request);
        guardUnsafe(context.title, context.content);
        Usage usage = consumeQuota(userId);
        SummaryResult result = summarizeContent(context);

        String markdown = result.markdown;
        if (isProviderModel(context.model)) {
            String providerMarkdown = callProviderForSummary(context, markdown);
            if (StringUtils.hasText(providerMarkdown)) {
                markdown = providerMarkdown;
            }
        }

        AiSummaryVO vo = new AiSummaryVO();
        vo.setModel(context.model);
        vo.setUsedCount(usage.used);
        vo.setRemainingCount(usage.remaining);
        vo.setSummaryType(context.summaryType);
        vo.setSummaryText(result.summaryText);
        vo.setMarkdown(markdown);
        vo.setMindMapImageData(result.mindMapImageData);
        vo.setMindMapFileName(result.mindMapFileName);
        return vo;
    }

    @Override
    public AiChatVO chat(Long userId, AiChatRequest request) {
        if (request == null || !StringUtils.hasText(request.getMessage())) {
            throw new BizException(400, "Message is required");
        }
        guardUnsafe(request.getMessage());
        if (!CollectionUtils.isEmpty(request.getHistory())) {
            for (AiChatRequest.ChatMessage item : request.getHistory()) {
                if (item != null) {
                    guardUnsafe(item.getContent());
                }
            }
        }
        Usage usage = consumeQuota(userId);

        String selectedModel = normalizeModel(request.getModel());
        RuntimeProviderConfig runtimeProviderConfig = resolveRuntimeProviderConfig(request.getModelConfig(), selectedModel);
        if (runtimeProviderConfig != null) {
            selectedModel = runtimeProviderConfig.provider;
        }
        String answerMarkdown;
        if (isProviderModel(selectedModel)) {
            answerMarkdown = callProviderForChat(selectedModel, request, runtimeProviderConfig);
        } else {
            answerMarkdown = buildChatMarkdown(request);
        }

        AiChatVO vo = new AiChatVO();
        vo.setModel(selectedModel);
        vo.setUsedCount(usage.used);
        vo.setRemainingCount(usage.remaining);
        vo.setAnswerMarkdown(answerMarkdown);
        return vo;
    }

    private QuestionContext resolveQuestionContext(Long userId, AiQuestionAnalysisRequest request) {
        QuestionContext ctx = new QuestionContext();
        ctx.model = normalizeModel(request == null ? null : request.getModel());
        ctx.runtimeProviderConfig = resolveRuntimeProviderConfig(
                request == null ? null : request.getModelConfig(),
                ctx.model
        );
        if (ctx.runtimeProviderConfig != null) {
            ctx.model = ctx.runtimeProviderConfig.provider;
        }
        ctx.errorCode = trim(request == null ? null : request.getErrorCode());
        ctx.exceptionMessage = trim(request == null ? null : request.getExceptionMessage());
        ctx.contextDescription = trim(request == null ? null : request.getContextDescription());
        ctx.language = trim(request == null ? null : request.getLanguage());
        ctx.tagNames = normalizeTags(request == null ? null : request.getTagNames());

        Long questionId = request == null ? null : request.getQuestionId();
        if (questionId != null) {
            QuestionDetailVO detail = questionService.detail(userId, questionId);
            if (!StringUtils.hasText(ctx.errorCode)) {
                ctx.errorCode = trim(detail.getErrorCode());
            }
            if (!StringUtils.hasText(ctx.exceptionMessage)) {
                ctx.exceptionMessage = trim(detail.getErrorReason());
            }
            if (!StringUtils.hasText(ctx.contextDescription)) {
                List<String> parts = new ArrayList<>();
                if (StringUtils.hasText(detail.getTitle())) {
                    parts.add("错题：" + detail.getTitle());
                }
                if (StringUtils.hasText(detail.getSource())) {
                    parts.add("来源：" + detail.getSource());
                }
                if (StringUtils.hasText(detail.getRemark())) {
                    parts.add("备注：" + detail.getRemark());
                }
                ctx.contextDescription = String.join("；", parts);
            }
            if (!StringUtils.hasText(ctx.language)) {
                ctx.language = trim(detail.getLanguage());
            }
            ctx.tagNames = mergeTags(ctx.tagNames, detail.getTagNames());
        }

        if (!StringUtils.hasText(ctx.language)) {
            ctx.language = "Java";
        }
        return ctx;
    }

    private SummaryContext resolveSummaryContext(Long userId, AiSummaryRequest request) {
        if (request == null) {
            throw new BizException(400, "Summary request is required");
        }

        SummaryContext ctx = new SummaryContext();
        ctx.model = normalizeModel(request.getModel());
        ctx.runtimeProviderConfig = resolveRuntimeProviderConfig(request.getModelConfig(), ctx.model);
        if (ctx.runtimeProviderConfig != null) {
            ctx.model = ctx.runtimeProviderConfig.provider;
        }
        ctx.summaryType = normalizeSummaryType(request.getSummaryType());
        ctx.title = trim(request.getTitle());
        ctx.content = trim(request.getContent());
        ctx.language = trim(request.getLanguage());
        ctx.tagNames = normalizeTags(request.getTagNames());

        if (request.getTargetId() != null) {
            String targetType = trim(request.getTargetType()).toUpperCase(Locale.ROOT);
            if ("NOTE".equals(targetType)) {
                NoteDetailVO detail = noteService.detail(userId, request.getTargetId());
                if (!StringUtils.hasText(ctx.title)) {
                    ctx.title = trim(detail.getTitle());
                }
                if (!StringUtils.hasText(ctx.content)) {
                    ctx.content = trim(detail.getContent());
                }
                if (!StringUtils.hasText(ctx.language)) {
                    ctx.language = trim(detail.getLanguage());
                }
                ctx.tagNames = mergeTags(ctx.tagNames, detail.getTagNames());
            } else if ("MATERIAL".equals(targetType)) {
                MaterialDetailVO detail = materialService.detail(userId, request.getTargetId());
                if (!SUPPORTED_MATERIAL_SUMMARY_TYPES.contains(String.valueOf(detail.getMaterialType()))) {
                    throw new BizException(400, "仅“知识点笔记/解题教程”类型资料支持 AI 总结");
                }
                if (!StringUtils.hasText(ctx.title)) {
                    ctx.title = trim(detail.getTitle());
                }
                if (!StringUtils.hasText(ctx.content)) {
                    ctx.content = trim(detail.getContent());
                }
                if (!StringUtils.hasText(ctx.language)) {
                    ctx.language = trim(detail.getLanguage());
                }
                ctx.tagNames = mergeTags(ctx.tagNames, detail.getTagNames());
            } else {
                throw new BizException(400, "targetType must be NOTE or MATERIAL");
            }
        }

        if (!StringUtils.hasText(ctx.title)) {
            ctx.title = "未命名内容";
        }
        if (!StringUtils.hasText(ctx.content)) {
            throw new BizException(400, "总结内容不能为空");
        }
        if (!StringUtils.hasText(ctx.language)) {
            ctx.language = "Java";
        }
        return ctx;
    }

    private SummaryResult summarizeContent(SummaryContext ctx) {
        if (SUMMARY_MIND_MAP.equals(ctx.summaryType)) {
            return summarizeMindMap(ctx);
        }
        if (SUMMARY_INTERVIEW.equals(ctx.summaryType)) {
            return summarizeInterview(ctx);
        }
        if (SUMMARY_FLASH_CARD.equals(ctx.summaryType)) {
            return summarizeFlashCard(ctx);
        }
        return summarizeCore(ctx);
    }

    private SummaryResult summarizeCore(SummaryContext ctx) {
        List<String> points = pickPoints(ctx.content, 5);
        StringBuilder markdown = new StringBuilder();
        markdown.append("### 📝 核心摘要\n");
        for (String point : points) {
            markdown.append("- ").append(point).append("\n");
        }
        markdown.append("\n### 📌 学习建议\n- 把每条结论配一段最小示例代码\n- 复习时先背结论再回看细节\n");

        SummaryResult result = new SummaryResult();
        result.summaryText = String.join("；", points);
        result.markdown = markdown.toString();
        return result;
    }

    private SummaryResult summarizeMindMap(SummaryContext ctx) {
        List<String> points = pickPoints(ctx.content, 6);
        StringBuilder markdown = new StringBuilder();
        markdown.append("### 🧠 思维导图\n- ").append(ctx.title).append("\n");
        for (String point : points) {
            markdown.append("  - ").append(point).append("\n");
        }

        SummaryResult result = new SummaryResult();
        result.summaryText = "已生成 " + points.size() + " 个导图分支";
        result.markdown = markdown.toString();
        result.mindMapImageData = buildMindMapImage(ctx.title, points);
        result.mindMapFileName = sanitizeFileName(ctx.title) + "_mindmap.svg";
        return result;
    }

    private SummaryResult summarizeInterview(SummaryContext ctx) {
        List<String> points = pickPoints(ctx.content, 3);
        StringBuilder markdown = new StringBuilder("### 🎯 面试考点\n");
        for (int i = 0; i < points.size(); i++) {
            markdown.append(i + 1).append(". **题目“").append(ctx.title).append("”的关键点是什么？**\n");
            markdown.append("   - 回答框架：定义 -> 场景 -> 落地实践（").append(points.get(i)).append("）\n");
        }

        SummaryResult result = new SummaryResult();
        result.summaryText = "已生成 " + points.size() + " 个面试问答框架";
        result.markdown = markdown.toString();
        return result;
    }

    private SummaryResult summarizeFlashCard(SummaryContext ctx) {
        List<String> points = pickPoints(ctx.content, 2);
        String card = buildFlashCard(ctx.title, points);

        SummaryResult result = new SummaryResult();
        result.summaryText = card;
        result.markdown = "### 🗂 速记卡片\n> " + card + "\n";
        return result;
    }

    private String buildFlashCard(String title, List<String> points) {
        StringBuilder card = new StringBuilder();
        card.append(shorten(title, 22)).append("：");
        for (int i = 0; i < points.size(); i++) {
            if (i > 0) {
                card.append("；");
            }
            card.append(points.get(i));
        }
        String text = card.toString();
        return text.length() > 100 ? text.substring(0, 100) : text;
    }

    private String buildChatMarkdown(AiChatRequest request) {
        String message = trim(request.getMessage());
        String lower = message.toLowerCase(Locale.ROOT);

        if (lower.contains("java") && lower.contains("泛型")) {
            return "### 🤖 AI 助手回复\n"
                    + "Java 泛型可以把类型检查前置到编译期，减少运行时类型转换异常。\n\n"
                    + "```java\n"
                    + "List<String> list = new ArrayList<>();\n"
                    + "list.add(\"hello\");\n"
                    + "String v = list.get(0);\n"
                    + "```\n"
                    + "\n- 进阶建议：区分 `? extends T` 与 `? super T` 的读写边界。";
        }

        if (lower.contains("spring") && lower.contains("ioc")) {
            return "### 🤖 AI 助手回复\n"
                    + "Spring IoC 的核心是把对象创建与依赖注入交给容器管理。\n\n"
                    + "1. 扫描并注册 Bean 定义\n"
                    + "2. 按依赖关系实例化对象\n"
                    + "3. 生命周期回调与扩展处理\n"
                    + "\n可继续追问：构造器注入和字段注入的差异。";
        }

        if (lower.contains("redis") && (lower.contains("过期") || lower.contains("ttl"))) {
            return "### 🤖 AI 助手回复\n"
                    + "Redis 过期策略通常是惰性删除 + 定期删除组合。\n"
                    + "建议在高并发场景加入随机过期抖动，减少同一时刻大量 key 失效。\n"
                    + "\n可继续追问：如何防止缓存击穿和缓存雪崩。";
        }

        String lastTopic = latestUserTopic(request.getHistory());
        StringBuilder markdown = new StringBuilder("### 🤖 AI 助手回复\n");
        if (StringUtils.hasText(lastTopic)) {
            markdown.append("结合你上一轮提到的“").append(lastTopic).append("”，建议按以下步骤推进：\n\n");
        } else {
            markdown.append("建议按“定位 -> 修复 -> 复盘”三步处理当前问题：\n\n");
        }

        markdown.append("1. 复现问题并记录关键输入与日志\n")
                .append("2. 在边界和异常分支增加保护代码\n")
                .append("3. 补一组回归测试并沉淀为笔记\n")
                .append("\n- 安全提醒：优先采用最小权限与参数校验，避免引入次生风险。");
        return markdown.toString();
    }

    private String latestUserTopic(List<AiChatRequest.ChatMessage> history) {
        if (CollectionUtils.isEmpty(history)) {
            return "";
        }
        for (int i = history.size() - 1; i >= 0; i--) {
            AiChatRequest.ChatMessage item = history.get(i);
            if (item == null) {
                continue;
            }
            if ("user".equalsIgnoreCase(trim(item.getRole())) && StringUtils.hasText(item.getContent())) {
                return shorten(item.getContent(), 18);
            }
        }
        return "";
    }

    private String detectExceptionType(String exceptionMessage, String errorCode) {
        String source = StringUtils.hasText(exceptionMessage) ? exceptionMessage : errorCode;
        if (!StringUtils.hasText(source)) {
            return "UnknownError";
        }
        Matcher matcher = EXCEPTION_TYPE_PATTERN.matcher(source);
        return matcher.find() ? matcher.group(1) : "UnknownError";
    }

    private String detectLocation(String exceptionMessage, String errorCode) {
        String source = StringUtils.hasText(exceptionMessage) ? exceptionMessage : errorCode;
        if (!StringUtils.hasText(source)) {
            return "请补充堆栈信息后再定位具体行号";
        }
        Matcher matcher = STACK_LINE_PATTERN.matcher(source);
        if (matcher.find()) {
            return "疑似第 " + matcher.group(1) + " 行（基于堆栈匹配）";
        }
        return "未匹配到明确行号，请重点检查最近修改的逻辑分支";
    }

    private String inferReason(String exceptionType, String contextDescription, List<String> tags) {
        String ex = String.valueOf(exceptionType).toLowerCase(Locale.ROOT);
        if (ex.contains("nullpointer")) {
            return "对象引用为空，访问前缺少 null 校验";
        }
        if (ex.contains("indexoutofbounds") || ex.contains("arrayindexoutofbounds")) {
            return "数组或集合访问越界，索引边界判断不足";
        }
        if (ex.contains("classcast")) {
            return "运行时类型不匹配，强转前缺少类型检查";
        }
        if (containsTag(tags, "redis") || String.valueOf(contextDescription).toLowerCase(Locale.ROOT).contains("redis")) {
            return "缓存键与过期策略设计不一致，导致数据读取路径异常";
        }
        return "输入校验与异常保护不足，导致运行期条件失效";
    }

    private String inferPrinciple(String exceptionType, String language) {
        String ex = String.valueOf(exceptionType).toLowerCase(Locale.ROOT);
        if (ex.contains("nullpointer")) {
            return "空指针是引用未指向实例造成的。建议在进入核心逻辑前统一做空值守卫，减少深层嵌套。";
        }
        if (ex.contains("indexoutofbounds") || ex.contains("arrayindexoutofbounds")) {
            return "边界错误往往来自循环条件与数据规模不一致。访问容器前应先确认有效区间。";
        }
        if (ex.contains("classcast")) {
            return "类型转换失败说明静态预期与运行时真实类型不一致。可通过泛型、instanceof 降低风险。";
        }
        if (String.valueOf(language).toLowerCase(Locale.ROOT).contains("python")) {
            return "Python 动态类型灵活但依赖输入校验，关键函数建议显式检查参数结构和类型。";
        }
        return "建议从输入校验、边界保护、异常分层三方面重构代码路径，提升可维护性。";
    }

    private String buildFixCode(String language, String originCode, List<String> tags) {
        String fence = codeFence(language);
        if ("python".equals(fence)) {
            return String.join("\n",
                    "def safe_handle(payload):",
                    "    if payload is None:",
                    "        return {'ok': False, 'reason': 'payload is empty'}",
                    "    data = payload.get('data') if isinstance(payload, dict) else None",
                    "    if data is None:",
                    "        return {'ok': False, 'reason': 'data missing'}",
                    "    return {'ok': True, 'data': data}");
        }
        if ("javascript".equals(fence)) {
            return String.join("\n",
                    "function safeHandle(payload) {",
                    "  if (!payload || typeof payload !== 'object') return { ok: false }",
                    "  const data = payload.data",
                    "  if (data == null) return { ok: false, reason: 'data missing' }",
                    "  return { ok: true, data }",
                    "}");
        }

        String hint = StringUtils.hasText(originCode) ? (" // 原始片段: " + shorten(originCode, 48)) : "";
        String tagHint = CollectionUtils.isEmpty(tags) ? "" : (" // 标签: " + String.join(", ", tags));
        return String.join("\n",
                "public Result safeHandle(Input input) {",
                "    if (input == null) {",
                "        return Result.fail(\"input is null\");",
                "    }",
                "    if (input.getItems() == null || input.getItems().isEmpty()) {",
                "        return Result.fail(\"items is empty\");",
                "    }",
                "    for (Item item : input.getItems()) {",
                "        if (item == null) continue;",
                "        // TODO: 核心逻辑",
                "    }",
                "    return Result.ok();",
                "}" + hint + tagHint);
    }

    private List<String> buildSuggestions(List<String> tags, String language) {
        List<String> list = new ArrayList<>();
        if (!CollectionUtils.isEmpty(tags)) {
            list.add("优先复习标签：" + String.join("、", tags));
        }
        list.add("建议查看“解题教程 / 知识点笔记”类型资料");
        list.add("把修复步骤沉淀为笔记，形成可复用模板");
        if (StringUtils.hasText(language)) {
            list.add("补充 " + language + " 的边界条件测试样例");
        }
        return list;
    }

    private String buildAnalysisMarkdown(QuestionContext context,
                                         String exceptionType,
                                         String location,
                                         String coreReason,
                                         String principle,
                                         String fixedCode,
                                         List<String> suggestions) {
        StringBuilder markdown = new StringBuilder();
        markdown.append("### 🔍 错误定位\n")
                .append("- 异常类型：").append(exceptionType).append("\n")
                .append("- 定位结论：").append(location).append("\n")
                .append("- 核心原因：").append(coreReason).append("\n\n")
                .append("### 💡 原理解析\n")
                .append(principle).append("\n\n")
                .append("### 🚀 修复方案\n")
                .append("```").append(codeFence(context.language)).append("\n")
                .append(fixedCode).append("\n```\n\n")
                .append("### 📚 关联建议\n");
        for (String suggestion : suggestions) {
            markdown.append("- ").append(suggestion).append("\n");
        }
        return markdown.toString();
    }

    private List<String> pickPoints(String content, int maxSize) {
        String normalized = String.valueOf(content).replace("\r", "\n").replace("\u0000", "").trim();
        if (!StringUtils.hasText(normalized)) {
            return List.of("内容为空，请补充主体文本");
        }
        String[] segments = normalized.split("[。！？\\n]+");
        LinkedHashSet<String> points = new LinkedHashSet<>();
        for (String seg : segments) {
            String point = trim(seg);
            if (!StringUtils.hasText(point) || point.length() < 6) {
                continue;
            }
            points.add(shorten(point, 46));
            if (points.size() >= maxSize) {
                break;
            }
        }
        if (points.isEmpty()) {
            points.add(shorten(normalized, 46));
        }
        return new ArrayList<>(points);
    }

    private String buildMindMapImage(String title, List<String> points) {
        int width = 920;
        int height = Math.max(280, 120 + points.size() * 78);

        StringBuilder svg = new StringBuilder();
        svg.append("<svg xmlns='http://www.w3.org/2000/svg' width='").append(width).append("' height='").append(height).append("'>")
                .append("<rect x='0' y='0' width='").append(width).append("' height='").append(height).append("' fill='#f6f9ff'/>")
                .append("<rect x='36' y='").append(height / 2 - 36).append("' width='220' height='72' rx='14' ry='14' fill='#1e40af'/>")
                .append("<text x='146' y='").append(height / 2 + 8).append("' text-anchor='middle' font-size='18' fill='white' font-family='Microsoft YaHei, PingFang SC, sans-serif'>")
                .append(xml(shorten(title, 12))).append("</text>");

        for (int i = 0; i < points.size(); i++) {
            int nodeY = 26 + i * 72;
            svg.append("<line x1='256' y1='").append(height / 2).append("' x2='316' y2='").append(nodeY + 20).append("' stroke='#94b4f8' stroke-width='2'/>")
                    .append("<rect x='316' y='").append(nodeY).append("' width='560' height='40' rx='10' ry='10' fill='white' stroke='#b9cef7'/>")
                    .append("<text x='332' y='").append(nodeY + 26).append("' font-size='14' fill='#1f2937' font-family='Microsoft YaHei, PingFang SC, sans-serif'>")
                    .append(xml(shorten(points.get(i), 32))).append("</text>");
        }
        svg.append("</svg>");
        return "data:image/svg+xml;base64," + Base64.getEncoder().encodeToString(svg.toString().getBytes(StandardCharsets.UTF_8));
    }

    private String codeFence(String language) {
        String value = trim(language).toLowerCase(Locale.ROOT);
        if (value.contains("python")) {
            return "python";
        }
        if (value.contains("front") || value.contains("javascript") || value.contains("js")) {
            return "javascript";
        }
        if (value.contains("c++") || value.contains("cpp")) {
            return "cpp";
        }
        if (value.contains("mysql") || value.contains("sql")) {
            return "sql";
        }
        return "java";
    }

    private List<String> mergeTags(List<String> source, List<String> incoming) {
        LinkedHashSet<String> set = new LinkedHashSet<>();
        set.addAll(normalizeTags(source));
        set.addAll(normalizeTags(incoming));
        return new ArrayList<>(set);
    }

    private List<String> normalizeTags(List<String> tags) {
        if (CollectionUtils.isEmpty(tags)) {
            return new ArrayList<>();
        }
        return tags.stream().map(this::trim).filter(StringUtils::hasText).distinct().collect(Collectors.toList());
    }

    private boolean containsTag(List<String> tags, String keyword) {
        if (CollectionUtils.isEmpty(tags) || !StringUtils.hasText(keyword)) {
            return false;
        }
        String lowerKeyword = keyword.toLowerCase(Locale.ROOT);
        for (String tag : tags) {
            if (trim(tag).toLowerCase(Locale.ROOT).contains(lowerKeyword)) {
                return true;
            }
        }
        return false;
    }

    private boolean isProviderModel(String model) {
        return MODEL_QWEN.equals(model) || MODEL_KIMI.equals(model) || MODEL_OPENAI.equals(model);
    }

    private boolean hasProviderKey(String model) {
        return StringUtils.hasText(providerApiKey(model));
    }

    private String callProviderForAnalysis(QuestionContext context, String fallbackMarkdown) {
        String systemPrompt = "你是编程错题分析助手。请用中文输出 Markdown，必须包含：错误定位、原理解析、修复方案、关联建议。";

        StringBuilder userPrompt = new StringBuilder();
        userPrompt.append("请分析以下错题上下文并给出结构化结论。\n")
                .append("语言：").append(context.language).append("\n")
                .append("标签：").append(context.tagNames == null ? "" : String.join("、", context.tagNames)).append("\n")
                .append("异常信息：").append(context.exceptionMessage).append("\n")
                .append("上下文：").append(context.contextDescription).append("\n")
                .append("错误代码：\n").append(context.errorCode).append("\n\n")
                .append("如果无法确认某细节，请明确写出“待确认”。");

        return callProviderMarkdown(context.model, context.runtimeProviderConfig, systemPrompt, userPrompt.toString(), fallbackMarkdown);
    }

    private String callProviderForSummary(SummaryContext context, String fallbackMarkdown) {
        String summaryTypeText;
        if (SUMMARY_MIND_MAP.equals(context.summaryType)) {
            summaryTypeText = "思维导图";
        } else if (SUMMARY_INTERVIEW.equals(context.summaryType)) {
            summaryTypeText = "面试考点";
        } else if (SUMMARY_FLASH_CARD.equals(context.summaryType)) {
            summaryTypeText = "速记卡片";
        } else {
            summaryTypeText = "核心提炼";
        }

        String systemPrompt = "你是学习总结助手。请用中文输出 Markdown，内容简洁、层次清晰、可直接复习。";
        String userPrompt = "请按“" + summaryTypeText + "”风格总结以下内容。\n"
                + "标题：" + context.title + "\n"
                + "语言：" + context.language + "\n"
                + "标签：" + (context.tagNames == null ? "" : String.join("、", context.tagNames)) + "\n"
                + "正文：\n" + context.content;

        return callProviderMarkdown(context.model, context.runtimeProviderConfig, systemPrompt, userPrompt, fallbackMarkdown);
    }

    private String callProviderForChat(String model, AiChatRequest request, RuntimeProviderConfig runtimeProviderConfig) {
        String systemPrompt = "你是编程学习助手。回答要准确、简洁，并优先给出可执行建议。";

        ArrayNode messages = objectMapper.createArrayNode();
        ObjectNode systemMessage = objectMapper.createObjectNode();
        systemMessage.put("role", "system");
        systemMessage.put("content", systemPrompt);
        messages.add(systemMessage);

        if (!CollectionUtils.isEmpty(request.getHistory())) {
            int start = Math.max(0, request.getHistory().size() - 8);
            for (int i = start; i < request.getHistory().size(); i++) {
                AiChatRequest.ChatMessage item = request.getHistory().get(i);
                if (item == null || !StringUtils.hasText(item.getContent())) {
                    continue;
                }
                String role = trim(item.getRole()).toLowerCase(Locale.ROOT);
                if (!"assistant".equals(role)) {
                    role = "user";
                }
                ObjectNode node = objectMapper.createObjectNode();
                node.put("role", role);
                node.put("content", item.getContent());
                messages.add(node);
            }
        }

        ObjectNode userMessage = objectMapper.createObjectNode();
        userMessage.put("role", "user");
        userMessage.put("content", request.getMessage());
        messages.add(userMessage);

        String answer = callProviderCompletion(model, runtimeProviderConfig, messages);
        if (!StringUtils.hasText(answer)) {
            throw new BizException(503, "AI 服务繁忙，请重试");
        }
        return answer;
    }

    private String callProviderMarkdown(String model,
                                        RuntimeProviderConfig runtimeProviderConfig,
                                        String systemPrompt,
                                        String userPrompt,
                                        String fallbackMarkdown) {
        ArrayNode messages = objectMapper.createArrayNode();

        ObjectNode systemMessage = objectMapper.createObjectNode();
        systemMessage.put("role", "system");
        systemMessage.put("content", systemPrompt);
        messages.add(systemMessage);

        ObjectNode userMessage = objectMapper.createObjectNode();
        userMessage.put("role", "user");
        userMessage.put("content", userPrompt);
        messages.add(userMessage);

        String answer = callProviderCompletion(model, runtimeProviderConfig, messages);
        return StringUtils.hasText(answer) ? answer : fallbackMarkdown;
    }

    private String callProviderCompletion(String model, RuntimeProviderConfig runtimeProviderConfig, ArrayNode messages) {
        String apiKey = firstNonBlank(
                runtimeProviderConfig == null ? "" : runtimeProviderConfig.apiKey,
                providerApiKey(model)
        );
        if (!StringUtils.hasText(apiKey)) {
            throw new BizException(400, providerLabel(model) + " API Key 未配置，请先在 AI 助手中填写或在 application.yml 中配置");
        }

        String endpoint = firstNonBlank(
                runtimeProviderConfig == null ? "" : runtimeProviderConfig.baseUrl,
                providerEndpoint(model)
        );
        endpoint = normalizeCompletionEndpoint(endpoint);

        String providerModel = firstNonBlank(
                runtimeProviderConfig == null ? "" : runtimeProviderConfig.modelName,
                providerModelName(model)
        );
        int timeoutMs = aiProviderProperties.getTimeoutMs() == null || aiProviderProperties.getTimeoutMs() <= 0
                ? 20000
                : aiProviderProperties.getTimeoutMs();

        ObjectNode body = objectMapper.createObjectNode();
        body.put("model", providerModel);
        body.set("messages", messages);
        body.put("temperature", 0.2d);

        try {
            if (!StringUtils.hasText(endpoint) || (!endpoint.startsWith("http://") && !endpoint.startsWith("https://"))) {
                throw new BizException(400, providerLabel(model) + " Endpoint 配置无效，请检查 application.yml");
            }
            HttpRequest httpRequest = HttpRequest.newBuilder(URI.create(endpoint))
                    .timeout(Duration.ofMillis(timeoutMs))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + apiKey)
                    .POST(HttpRequest.BodyPublishers.ofString(body.toString(), StandardCharsets.UTF_8))
                    .build();

            HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                String providerErrorMessage = extractProviderErrorMessage(response.body());
                if (StringUtils.hasText(providerErrorMessage)) {
                    throw new BizException(503, providerLabel(model) + " 调用失败(" + response.statusCode() + ")：" + providerErrorMessage);
                }
                throw new BizException(503, providerLabel(model) + " 服务调用失败(" + response.statusCode() + ")，请检查 API Key/模型/Endpoint 配置");
            }

            JsonNode root = objectMapper.readTree(response.body());
            String content = extractProviderContent(root);
            if (!StringUtils.hasText(content)) {
                throw new BizException(503, providerLabel(model) + " 返回结果为空，请重试");
            }
            return content;
        } catch (BizException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new BizException(503, providerLabel(model) + " 服务繁忙，请重试");
        }
    }

    private String providerApiKey(String model) {
        AiProviderProperties.Provider provider = provider(model);
        return provider == null ? "" : trim(provider.getApiKey());
    }

    private String providerEndpoint(String model) {
        AiProviderProperties.Provider provider = provider(model);
        String configured = provider == null ? "" : trim(provider.getEndpoint());
        if (StringUtils.hasText(configured)) {
            return normalizeCompletionEndpoint(configured);
        }
        if (MODEL_QWEN.equals(model)) {
            return "https://dashscope.aliyuncs.com/compatible-mode/v1/chat/completions";
        }
        if (MODEL_KIMI.equals(model)) {
            return "https://api.moonshot.cn/v1/chat/completions";
        }
        return "https://api.openai.com/v1/chat/completions";
    }

    private String providerModelName(String model) {
        AiProviderProperties.Provider provider = provider(model);
        String configured = provider == null ? "" : trim(provider.getModel());
        if (StringUtils.hasText(configured)) {
            return configured;
        }
        if (MODEL_QWEN.equals(model)) {
            return "qwen-plus";
        }
        if (MODEL_KIMI.equals(model)) {
            return "moonshot-v1-8k";
        }
        return "gpt-4o-mini";
    }

    private String providerLabel(String model) {
        if (MODEL_QWEN.equals(model)) {
            return "Qwen";
        }
        if (MODEL_KIMI.equals(model)) {
            return "Kimi";
        }
        if (MODEL_OPENAI.equals(model)) {
            return "OpenAI";
        }
        return "模型服务";
    }


    private String normalizeCompletionEndpoint(String endpoint) {
        String value = trim(endpoint);
        if (!StringUtils.hasText(value)) {
            return value;
        }
        String lower = value.toLowerCase(Locale.ROOT);
        if (lower.contains("/chat/completions")) {
            return value;
        }
        if (value.endsWith("/")) {
            value = value.substring(0, value.length() - 1);
        }
        return value + "/chat/completions";
    }

    private String extractProviderErrorMessage(String body) {
        String normalized = trim(body);
        if (!StringUtils.hasText(normalized)) {
            return "";
        }
        try {
            JsonNode root = objectMapper.readTree(normalized);
            String message = firstNonBlank(
                    root.path("error").path("message").asText(""),
                    root.path("message").asText(""),
                    root.path("error_msg").asText(""),
                    root.path("msg").asText("")
            );
            if (StringUtils.hasText(message)) {
                return shorten(message.replaceAll("\\s+", " "), 140);
            }
        } catch (Exception ignored) {
            // ignore parse exception and fallback to body text.
        }
        return shorten(normalized.replaceAll("\\s+", " "), 140);
    }

    private String extractProviderContent(JsonNode root) {
        JsonNode choices = root.path("choices");
        if (choices.isArray() && choices.size() > 0) {
            String messageContent = extractContentNodeText(choices.path(0).path("message").path("content"));
            if (StringUtils.hasText(messageContent)) {
                return messageContent;
            }
            String textContent = extractContentNodeText(choices.path(0).path("text"));
            if (StringUtils.hasText(textContent)) {
                return textContent;
            }
        }

        String outputText = trim(root.path("output_text").asText(""));
        if (StringUtils.hasText(outputText)) {
            return outputText;
        }
        return "";
    }

    private String extractContentNodeText(JsonNode node) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return "";
        }
        if (node.isTextual()) {
            return trim(node.asText(""));
        }
        if (node.isArray()) {
            StringBuilder sb = new StringBuilder();
            for (JsonNode part : node) {
                if (part == null || part.isNull()) {
                    continue;
                }
                if (part.isTextual()) {
                    if (StringUtils.hasText(part.asText())) {
                        if (sb.length() > 0) {
                            sb.append("\n");
                        }
                        sb.append(part.asText());
                    }
                    continue;
                }
                String text = trim(part.path("text").asText(""));
                if (StringUtils.hasText(text)) {
                    if (sb.length() > 0) {
                        sb.append("\n");
                    }
                    sb.append(text);
                }
            }
            return trim(sb.toString());
        }
        return "";
    }

    private String firstNonBlank(String... values) {
        if (values == null) {
            return "";
        }
        for (String value : values) {
            if (StringUtils.hasText(value)) {
                return value;
            }
        }
        return "";
    }

    private RuntimeProviderConfig resolveRuntimeProviderConfig(AiModelConfig requestModelConfig, String fallbackModel) {
        if (requestModelConfig == null) {
            return null;
        }
        String providerModel = normalizeModel(firstNonBlank(
                requestModelConfig.getProvider(),
                fallbackModel
        ));
        if (!isProviderModel(providerModel)) {
            return null;
        }

        RuntimeProviderConfig config = new RuntimeProviderConfig();
        config.provider = providerModel;
        config.baseUrl = trim(requestModelConfig.getBaseUrl());
        config.apiKey = trim(requestModelConfig.getApiKey());
        config.modelName = trim(requestModelConfig.getModelName());
        return config;
    }

    private AiProviderProperties.Provider provider(String model) {
        if (MODEL_QWEN.equals(model)) {
            return aiProviderProperties.getQwen();
        }
        if (MODEL_KIMI.equals(model)) {
            return aiProviderProperties.getKimi();
        }
        if (MODEL_OPENAI.equals(model)) {
            return aiProviderProperties.getOpenai();
        }
        return null;
    }

    private String normalizeModel(String model) {
        String value = trim(model).toUpperCase(Locale.ROOT);
        if (SUPPORTED_MODELS.contains(value)) {
            return value;
        }
        return DEFAULT_MODEL;
    }

    private String normalizeSummaryType(String type) {
        String value = trim(type).toUpperCase(Locale.ROOT);
        if (SUMMARY_MIND_MAP.equals(value) || SUMMARY_INTERVIEW.equals(value) || SUMMARY_FLASH_CARD.equals(value)) {
            return value;
        }
        return SUMMARY_CORE_EXTRACT;
    }

    private AiModelCatalogVO.ModelOption model(String value, String label, String description) {
        AiModelCatalogVO.ModelOption option = new AiModelCatalogVO.ModelOption();
        option.setValue(value);
        option.setLabel(label);
        option.setDescription(description);
        return option;
    }

    private synchronized Usage consumeQuota(Long userId) {
        LocalDate today = LocalDate.now(SHANGHAI_ZONE);
        DailyUsageRecord record = usageByUser.get(userId);
        if (record == null || !today.equals(record.date)) {
            record = new DailyUsageRecord();
            record.date = today;
            record.usedCount = 0;
            usageByUser.put(userId, record);
        }
        if (record.usedCount >= FREE_DAILY_LIMIT) {
            throw new BizException(429, "今日 AI 免费额度已用尽，请明天再试或联系客服升级");
        }
        record.usedCount += 1;
        Usage usage = new Usage();
        usage.used = record.usedCount;
        usage.remaining = FREE_DAILY_LIMIT - record.usedCount;
        return usage;
    }

    private void guardUnsafe(String... values) {
        if (values == null) {
            return;
        }
        for (String raw : values) {
            String normalized = trim(raw).toLowerCase(Locale.ROOT);
            if (!StringUtils.hasText(normalized)) {
                continue;
            }
            for (String blocked : BLOCKED_TERMS) {
                if (normalized.contains(blocked.toLowerCase(Locale.ROOT))) {
                    throw new BizException(400, "检测到潜在违规内容，请调整后重试");
                }
            }
        }
    }

    private String trim(String value) {
        return value == null ? "" : value.trim();
    }

    private String shorten(String value, int length) {
        String text = trim(value).replace("\n", " ").replace("\r", " ");
        if (text.length() <= length) {
            return text;
        }
        return text.substring(0, length) + "...";
    }

    private String sanitizeFileName(String value) {
        String text = trim(value);
        if (!StringUtils.hasText(text)) {
            return "ai_summary";
        }
        return shorten(text.replaceAll("[\\\\/:*?\"<>|]", "_"), 30);
    }

    private String xml(String value) {
        return String.valueOf(value)
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;");
    }

    private static class QuestionContext {
        private String model;
        private RuntimeProviderConfig runtimeProviderConfig;
        private String errorCode;
        private String exceptionMessage;
        private String contextDescription;
        private String language;
        private List<String> tagNames;
    }

    private static class SummaryContext {
        private String model;
        private RuntimeProviderConfig runtimeProviderConfig;
        private String summaryType;
        private String title;
        private String content;
        private String language;
        private List<String> tagNames;
    }

    private static class RuntimeProviderConfig {
        private String provider;
        private String baseUrl;
        private String apiKey;
        private String modelName;
    }

    private static class SummaryResult {
        private String summaryText;
        private String markdown;
        private String mindMapImageData;
        private String mindMapFileName;
    }

    private static class DailyUsageRecord {
        private LocalDate date;
        private int usedCount;
    }

    private static class Usage {
        private int used;
        private int remaining;
    }
}

