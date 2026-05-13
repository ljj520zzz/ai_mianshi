package com.ljj.aiagent.app;

import com.ljj.aiagent.advisor.MyLoggerAdvisor;
import com.ljj.aiagent.chatmemory.FileBasedChatMemory;
import com.ljj.aiagent.mapper.ConversationMapper;
import com.ljj.aiagent.mapper.PromptConfigMapper;
import com.ljj.aiagent.rag.QueryRewriter;
import com.ljj.aiagent.tools.PDFGenerationTool;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;

@Component
@Slf4j
public class LoveApp {

    private final ChatClient chatClient;
    private final PDFGenerationTool pdfGenerationTool = new PDFGenerationTool();
    private static final String SYSTEM_PROMPT_NAME = "SYSTEM_PROMPT";
    private static final String FINISH_INTERVIEW_COMMAND = "结束面试";
    private static final List<String> FINISH_INTERVIEW_KEYWORDS = List.of(
            "结束面试",
            "面试结束",
            "结束",
            "结束了",
            "结束吧",
            "结束一下"
    );
    private static final List<String> FINISH_ACTION_KEYWORDS = List.of(
            "结束", "停止", "终止", "收尾", "截止", "完成"
    );
    private static final List<String> INTERVIEW_CONTEXT_KEYWORDS = List.of(
            "面试", "本轮", "这一轮", "当前轮", "本次"
    );
    private static final List<String> REPORT_KEYWORDS = List.of(
            "报告", "总结", "复盘", "评价", "反馈", "pdf", "下载", "导出", "生成文件"
    );
    private static final List<String> SUMMARY_SECTION_TITLES = List.of(
            "总体表现", "核心优势", "主要短板", "岗位匹配度", "改进计划", "下一轮模拟建议"
    );
    @Resource
    private ConversationMapper conversationMapper;
    @Resource
    private PromptConfigMapper promptConfigMapper;


    private static final String DEFAULT_SYSTEM_PROMPT = "你是AI面试官，专注于求职面试辅导。严禁扮演恋爱顾问、情感咨询师或任何与恋爱相关角色。" +
            "你必须先获取并记住用户的身份信息和专业背景（如：在校生/应届/社招、专业、目标岗位、工作年限）。" +
            "如果信息不完整，先追问补齐，再开始出题；信息完整后必须进行定制化面试。" +
            "每轮面试流程固定为：1）先出1道题；2）等待用户回答；3）基于回答给出评价（优点/问题/改进建议）；4）给出该题参考答案。" +
            "题目需贴合用户身份和专业，覆盖简历题、技术题、行为题，难度由浅入深。" +
            "输出简洁清晰，必须使用 Markdown 二级标题分段：## 题目、## 评价、## 参考答案；各板块之间空一行，题目正文紧跟在“## 题目”下一行。只回答面试相关话题，不回答恋爱、情感类问题。";

    /**
     * 初始化 ChatClient
     *
     * @param dashscopeChatModel
     */
    public LoveApp(ChatModel dashscopeChatModel) {
        // 初始化基于文件的对话记忆
        String fileDir = System.getProperty("user.dir") + "/tmp/chat-memory";
        ChatMemory chatMemory = new FileBasedChatMemory(fileDir);
        chatClient = ChatClient.builder(dashscopeChatModel)
                .defaultAdvisors(
                        MessageChatMemoryAdvisor.builder(chatMemory).build(),
                        // 自定义日志 Advisor，可按需开启
                        new MyLoggerAdvisor()
//                        // 自定义推理增强 Advisor，可按需开启
//                       ,new ReReadingAdvisor()
                )
                .build();
    }

    /**
     * AI 基础对话（支持多轮对话记忆）
     *
     * @param message
     * @param chatId
     * @return
     */
    public String doChat(String message, String chatId) {
        if (isFinishInterviewCommand(message)) {
            return generateInterviewReportAndPdf(chatId);
        }
        ChatResponse chatResponse = chatClient
                .prompt()
                .system(resolveSystemPrompt(chatId))
                .user(message)
                .advisors(spec -> spec.param(ChatMemory.CONVERSATION_ID, chatId))
                .call()
                .chatResponse();
        String content = chatResponse.getResult().getOutput().getText();
        log.info("content: {}", content);
        return content;
    }

    /**
     * AI 基础对话（支持多轮对话记忆，SSE 流式传输）
     *
     * @param message
     * @param chatId
     * @return
     */
    public Flux<String> doChatByStream(String message, String chatId) {
        if (isFinishInterviewCommand(message)) {
            return generateInterviewReportAndPdfByStream(chatId);
        }
        return chatClient
                .prompt()
                .system(resolveSystemPrompt(chatId))
                .user(message)
                .advisors(spec -> spec.param(ChatMemory.CONVERSATION_ID, chatId))
                .stream()
                .content();
    }

    private Flux<String> generateInterviewReportAndPdfByStream(String chatId) {
        String safeChatId = (chatId == null || chatId.isBlank()) ? "default" : chatId.trim();
        String sessionCode = extractSessionCode(safeChatId);
        String historyReport = findFinalReportFromHistory(sessionCode, safeChatId);
        if (historyReport != null && !historyReport.isBlank()) {
            return Flux.just(historyReport)
                    .concatWith(Mono.fromSupplier(() -> "\n\n" + buildPdfResultMessage(safeChatId, historyReport)));
        }
        String conversationContext = buildConversationContextFromDb(sessionCode, safeChatId);
        if (conversationContext == null || conversationContext.isBlank()) {
            return Flux.just("当前会话暂无可导出的综合评价报告。请先进行面试问答后，再发送“结束面试”。");
        }
        StringBuilder summaryBuilder = new StringBuilder();
        return chatClient
                .prompt()
                .system(resolveSystemPrompt(chatId) + "用户已结束面试，你要基于提供的完整对话记录生成最终综合评价报告，不要要求用户重复提供历史信息。禁止继续出题，禁止追加新的面试问题，禁止输出“题目/参考答案”等出题结构。")
                .user("以下是本次面试完整对话记录：\n" + conversationContext + "\n\n请输出综合评价报告，必须且仅包含以下小标题：总体表现、核心优势、主要短板、岗位匹配度、改进计划（7天）、下一轮模拟建议。不要再给任何新题目。")
                .stream()
                .content()
                .doOnNext(chunk -> {
                    if (chunk != null) {
                        summaryBuilder.append(chunk);
                    }
                })
                .concatWith(Mono.fromSupplier(() -> {
                    String summary = extractPureReportContent(summaryBuilder.toString());
                    if (summary.isBlank()) {
                        return "";
                    }
                    return "\n\n" + buildPdfResultMessage(safeChatId, summary);
                }));
    }

    record LoveReport(String title, List<String> suggestions) {

    }

    /**
     * AI 面试报告功能（实战结构化输出）
     *
     * @param message
     * @param chatId
     * @return
     */
    public LoveReport doChatWithReport(String message, String chatId) {
        LoveReport loveReport = chatClient
                .prompt()
                .system(resolveSystemPrompt(chatId) + "每次对话后都要生成面试反馈，标题为{用户名}的面试报告，内容为建议列表")
                .user(message)
                .advisors(spec -> spec.param(ChatMemory.CONVERSATION_ID, chatId))
                .call()
                .entity(LoveReport.class);
        log.info("loveReport: {}", loveReport);
        return loveReport;
    }

    private boolean isFinishInterviewCommand(String message) {
        if (message == null) {
            return false;
        }
        String normalized = message.trim().toLowerCase();
        if (normalized.isEmpty()) {
            return false;
        }
        if (FINISH_INTERVIEW_COMMAND.equals(normalized)) {
            return true;
        }
        if (containsAny(normalized, FINISH_INTERVIEW_KEYWORDS)) {
            return true;
        }
        boolean hasFinishAction = containsAny(normalized, FINISH_ACTION_KEYWORDS);
        boolean hasInterviewContext = containsAny(normalized, INTERVIEW_CONTEXT_KEYWORDS);
        boolean hasReportIntent = containsAny(normalized, REPORT_KEYWORDS);
        return (hasFinishAction && hasInterviewContext) || (hasFinishAction && hasReportIntent);
    }

    private boolean containsAny(String content, List<String> keywords) {
        return keywords.stream().anyMatch(content::contains);
    }

    private String generateInterviewReportAndPdf(String chatId) {
        String safeChatId = (chatId == null || chatId.isBlank()) ? "default" : chatId.trim();
        String sessionCode = extractSessionCode(safeChatId);
        String finalReportContent = findFinalReportFromHistory(sessionCode, safeChatId);
        if (finalReportContent == null || finalReportContent.isBlank()) {
            String conversationContext = buildConversationContextFromDb(sessionCode, safeChatId);
            if (conversationContext != null && !conversationContext.isBlank()) {
                finalReportContent = generateSummaryReportFromConversation(chatId, conversationContext);
            }
        }
        if (finalReportContent == null || finalReportContent.isBlank()) {
            return "当前会话暂无可导出的综合评价报告。请先生成并确认最终报告内容后，再导出 PDF。";
        }
        return finalReportContent + "\n\n" + buildPdfResultMessage(safeChatId, finalReportContent);
    }

    private String buildPdfResultMessage(String safeChatId, String finalReportContent) {
        String reportFileName = writeInterviewReportPdf(safeChatId, finalReportContent);
        if (reportFileName == null || reportFileName.isBlank()) {
            return "Error generating PDF: 暂无可导出的综合评价报告";
        }
        if (reportFileName.startsWith("Error generating PDF:")) {
            return reportFileName;
        }
        return "PDF结果：" + reportFileName;
    }

    public String exportInterviewReportPdf(String chatId) {
        return exportInterviewReportPdf(chatId, null);
    }

    public String exportInterviewReportPdf(String chatId, String providedReportContent) {
        String safeChatId = normalizeChatId(chatId);
        String finalReportContent = null;
        if (providedReportContent != null && !providedReportContent.isBlank()) {
            finalReportContent = extractPureReportContent(providedReportContent);
        }
        if (finalReportContent == null || finalReportContent.isBlank()) {
            String sessionCode = extractSessionCode(safeChatId);
            finalReportContent = findFinalReportFromHistory(sessionCode, safeChatId);
            if (finalReportContent == null || finalReportContent.isBlank()) {
                String conversationContext = buildConversationContextFromDb(sessionCode, safeChatId);
                if (conversationContext != null && !conversationContext.isBlank()) {
                    finalReportContent = generateSummaryReportFromConversation(chatId, conversationContext);
                }
            }
        }
        if (finalReportContent == null || finalReportContent.isBlank()) {
            return null;
        }
        return writeInterviewReportPdf(safeChatId, finalReportContent);
    }

    private String writeInterviewReportPdf(String safeChatId, String finalReportContent) {
        String nickname = resolveUserNickname(safeChatId);
        String pdfContent = appendGreetingBelowTitle(finalReportContent, nickname);
        String reportFileName = buildReportFileName(safeChatId);
        return pdfGenerationTool.generatePDF(reportFileName, pdfContent);
    }

    private String buildReportFileName(String safeChatId) {
        return "interview_report_" + safeChatId.replaceAll("[^A-Za-z0-9._-]", "_") + ".pdf";
    }

    private String normalizeChatId(String chatId) {
        return (chatId == null || chatId.isBlank()) ? "default" : chatId.trim();
    }

    private String extractSessionCode(String chatId) {
        int firstUnderscoreIndex = chatId.indexOf('_');
        if (firstUnderscoreIndex < 0 || firstUnderscoreIndex >= chatId.length() - 1) {
            return chatId;
        }
        return chatId.substring(firstUnderscoreIndex + 1);
    }

    private List<String> buildSessionKeys(String sessionCode, String chatId) {
        if (sessionCode == null || sessionCode.isBlank()) {
            return List.of();
        }
        List<String> keys = new ArrayList<>();
        keys.add(sessionCode);
        if (chatId != null && !chatId.isBlank() && !chatId.equals(sessionCode)) {
            keys.add(chatId);
        }
        int lastUnderscoreIndex = chatId == null ? -1 : chatId.lastIndexOf('_');
        if (lastUnderscoreIndex >= 0 && lastUnderscoreIndex < chatId.length() - 1) {
            String lastSegment = chatId.substring(lastUnderscoreIndex + 1);
            if (!lastSegment.equals(sessionCode) && !lastSegment.equals(chatId)) {
                keys.add(lastSegment);
            }
        }
        return keys;
    }

    private String findFinalReportFromHistory(String sessionCode, String chatId) {
        List<String> keys = buildSessionKeys(sessionCode, chatId);
        for (String key : keys) {
            List<String> assistantContents;
            try {
                assistantContents = conversationMapper.listLatestAssistantContents(key);
            } catch (Exception e) {
                log.error("查询最近助手消息失败, sessionCode={}", key, e);
                continue;
            }
            if (assistantContents == null || assistantContents.isEmpty()) {
                continue;
            }
            for (String content : assistantContents) {
                String cleaned = extractPureReportContent(content);
                if (isStructuredSummaryReport(cleaned)) {
                    return cleaned;
                }
            }
        }
        return "";
    }

    private String buildConversationContextFromDb(String sessionCode, String chatId) {
        List<String> keys = buildSessionKeys(sessionCode, chatId);
        for (String key : keys) {
            List<ConversationMapper.ConversationRow> rows;
            try {
                rows = conversationMapper.listConversationRows(key);
            } catch (Exception e) {
                log.error("查询会话历史失败, sessionCode={}", key, e);
                continue;
            }
            if (rows == null || rows.isEmpty()) {
                continue;
            }
            StringBuilder context = new StringBuilder();
            for (ConversationMapper.ConversationRow row : rows) {
                String role = row.getRoleType() == null ? "assistant" : row.getRoleType();
                String content = row.getContent() == null ? "" : row.getContent();
                if (content.isBlank()) {
                    continue;
                }
                if ("user".equalsIgnoreCase(role)) {
                    context.append("用户：").append(content).append("\n");
                } else {
                    context.append("面试官：").append(content).append("\n");
                }
            }
            if (!context.isEmpty()) {
                return context.toString().trim();
            }
        }
        return "";
    }

    private String generateSummaryReportFromConversation(String chatId, String conversationContext) {
        try {
            String reportContent = chatClient
                    .prompt()
                    .system(resolveSystemPrompt(chatId) + "用户已结束面试，你要基于提供的完整对话记录生成最终综合评价报告，不要要求用户重复提供历史信息。禁止继续出题，禁止追加新的面试问题，禁止输出“题目/参考答案”等出题结构。")
                    .user("以下是本次面试完整对话记录：\n" + conversationContext + "\n\n请输出综合评价报告，必须且仅包含以下小标题：总体表现、核心优势、主要短板、岗位匹配度、改进计划（7天）、下一轮模拟建议。不要再给任何新题目。")
                    .call()
                    .content();
            if (reportContent == null || reportContent.isBlank()) {
                return "";
            }
            return extractPureReportContent(reportContent);
        } catch (Exception e) {
            log.error("生成综合评价报告失败, chatId={}", chatId, e);
            return "";
        }
    }

    private String extractPureReportContent(String content) {
        if (content == null || content.isBlank()) {
            return "";
        }
        String normalized = content.replace("\r\n", "\n").trim();
        normalized = normalized.replaceAll("\\n+PDF结果[:：][\\s\\S]*$", "").trim();
        if (normalized.startsWith("当前会话暂无") || normalized.startsWith("本次面试已结束，但未生成有效综合评价内容")) {
            return "";
        }
        return normalized;
    }

    private boolean isStructuredSummaryReport(String content) {
        if (content == null || content.isBlank()) {
            return false;
        }
        String normalized = content.replace("\r\n", "\n");
        int matchedCount = 0;
        for (String title : SUMMARY_SECTION_TITLES) {
            if (normalized.contains(title)) {
                matchedCount++;
            }
        }
        return matchedCount >= 3;
    }

    private String resolveUserNickname(String chatId) {
        String userId = extractUserId(chatId);
        if (userId == null || userId.isBlank()) {
            return "同学";
        }
        try {
            String nickname = conversationMapper.findNicknameByUserId(userId);
            if (nickname == null || nickname.isBlank()) {
                return "同学";
            }
            return nickname.trim();
        } catch (Exception e) {
            log.error("查询用户昵称失败, userId={}", userId, e);
            return "同学";
        }
    }

    private String appendGreetingBelowTitle(String reportContent, String nickname) {
        String normalized = reportContent == null ? "" : reportContent.replace("\r\n", "\n").trim();
        if (normalized.isBlank()) {
            return "您好，" + nickname + "，这是您的综合评价报告";
        }
        String greeting = "您好，" + nickname + "，这是您的综合评价报告";
        String[] lines = normalized.split("\n", -1);
        int titleIndex = -1;
        for (int i = 0; i < lines.length; i++) {
            if (!lines[i].trim().isEmpty()) {
                titleIndex = i;
                break;
            }
        }
        if (titleIndex < 0) {
            return greeting;
        }
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < lines.length; i++) {
            if (i > 0) {
                builder.append("\n");
            }
            builder.append(lines[i]);
            if (i == titleIndex) {
                builder.append("\n").append(greeting);
            }
        }
        return builder.toString();
    }

    private String resolveSystemPrompt(String chatId) {
        String defaultPrompt = DEFAULT_SYSTEM_PROMPT;
        try {
            String userId = extractUserId(chatId);
            if (userId != null) {
                String userPrompt = promptConfigMapper.findLatestPromptContentByUserIdAndName(userId, SYSTEM_PROMPT_NAME);
                if (userPrompt != null && !userPrompt.trim().isEmpty()) {
                    return defaultPrompt + "\n\n" + userPrompt.trim();
                }
            }
        } catch (Exception e) {
            log.error("读取系统提示词失败, 使用默认提示词", e);
        }
        return defaultPrompt;
    }

    private String extractUserId(String chatId) {
        if (chatId == null || chatId.isBlank()) {
            return null;
        }
        String value = chatId.trim();
        int idx = value.indexOf('_');
        if (idx <= 0) {
            return null;
        }
        String userId = value.substring(0, idx).trim();
        return userId.isEmpty() ? null : userId;
    }

    // AI 面试知识库问答功能

    @Resource
    private VectorStore loveAppVectorStore;

    @Resource
    private Advisor loveAppRagCloudAdvisor;

    @Resource
    private VectorStore pgVectorVectorStore;

    @Resource
    private QueryRewriter queryRewriter;

    /**
     * 和 RAG 知识库进行对话
     *
     * @param message
     * @param chatId
     * @return
     */
    public String doChatWithRag(String message, String chatId) {
        // 查询重写
        String rewrittenMessage = queryRewriter.doQueryRewrite(message);
        ChatResponse chatResponse = chatClient
                .prompt()
                .system(resolveSystemPrompt(chatId))
                // 使用改写后的查询
                .user(rewrittenMessage)
                .advisors(spec -> spec.param(ChatMemory.CONVERSATION_ID, chatId))
                // 开启日志，便于观察效果
                .advisors(new MyLoggerAdvisor())
                // 应用 PgVector RAG 知识库问答，支持后台刷新后立即生效
                .advisors(new QuestionAnswerAdvisor(pgVectorVectorStore))
                // 应用 RAG 检索增强服务（基于云知识库服务）
//                .advisors(loveAppRagCloudAdvisor)
                // 应用 RAG 检索增强服务（基于内存向量存储）
//                .advisors(new QuestionAnswerAdvisor(loveAppVectorStore))
                // 应用自定义的 RAG 检索增强服务（文档查询器 + 上下文增强器）
//                .advisors(
//                        LoveAppRagCustomAdvisorFactory.createLoveAppRagCustomAdvisor(
//                                loveAppVectorStore, "单身"
//                        )
//                )
                .call()
                .chatResponse();
        String content = chatResponse.getResult().getOutput().getText();
        log.info("content: {}", content);
        return content;
    }

    // AI 调用工具能力
    @Resource
    private ToolCallback[] allTools;

    /**
     * AI 面试报告功能（支持调用工具）
     *
     * @param message
     * @param chatId
     * @return
     */
    public String doChatWithTools(String message, String chatId) {
        ChatResponse chatResponse = chatClient
                .prompt()
                .system(resolveSystemPrompt(chatId))
                .user(message)
                .advisors(spec -> spec.param(ChatMemory.CONVERSATION_ID, chatId))
                // 开启日志，便于观察效果
                .advisors(new MyLoggerAdvisor())
                .toolCallbacks(allTools)
                .call()
                .chatResponse();
        String content = chatResponse.getResult().getOutput().getText();
        log.info("content: {}", content);
        return content;
    }

    // AI 调用 MCP 服务

    @Resource
    private ToolCallbackProvider toolCallbackProvider;

    /**
     * AI 面试报告功能（调用 MCP 服务）
     *
     * @param message
     * @param chatId
     * @return
     */
    public String doChatWithMcp(String message, String chatId) {
        ChatResponse chatResponse = chatClient
                .prompt()
                .system(resolveSystemPrompt(chatId))
                .user(message)
                .advisors(spec -> spec.param(ChatMemory.CONVERSATION_ID, chatId))
                // 开启日志，便于观察效果
                .advisors(new MyLoggerAdvisor())
                .toolCallbacks(toolCallbackProvider)
                .call()
                .chatResponse();
        String content = chatResponse.getResult().getOutput().getText();
        log.info("content: {}", content);
        return content;
    }
}
