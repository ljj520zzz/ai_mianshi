package com.ljj.aiagent.rag;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.markdown.MarkdownDocumentReader;
import org.springframework.ai.reader.markdown.config.MarkdownDocumentReaderConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

/**
 * 面试官应用文档加载器
 */
@Component
@Slf4j
public class LoveAppDocumentLoader {

    private final ResourcePatternResolver resourcePatternResolver;
    private final String knowledgeDir;

    public LoveAppDocumentLoader(ResourcePatternResolver resourcePatternResolver,
                                 @Value("${app.knowledge-dir:}") String knowledgeDir) {
        this.resourcePatternResolver = resourcePatternResolver;
        this.knowledgeDir = knowledgeDir == null ? "" : knowledgeDir.trim();
    }

    /**
     * 加载多篇 Markdown 文档（与 AdminKnowledgeService 写入目录一致）
     */
    public List<Document> loadMarkdowns() {
        if (knowledgeDir.isEmpty()) {
            return loadFromClasspath();
        }
        return loadFromFilesystem(Paths.get(knowledgeDir));
    }

    private List<Document> loadFromClasspath() {
        List<Document> allDocuments = new ArrayList<>();
        try {
            Resource[] resources = resourcePatternResolver.getResources("classpath:document/*.md");
            for (Resource resource : resources) {
                String filename = resource.getFilename();
                allDocuments.addAll(parseMarkdown(resource, filename));
            }
        } catch (IOException e) {
            log.error("Markdown 文档加载失败", e);
        }
        return allDocuments;
    }

    private List<Document> loadFromFilesystem(Path dir) {
        List<Document> allDocuments = new ArrayList<>();
        Path absolute = dir.toAbsolutePath().normalize();
        if (!Files.isDirectory(absolute)) {
            log.warn("知识库目录不存在或不是目录: {}", absolute);
            return allDocuments;
        }
        try (Stream<Path> stream = Files.list(absolute)) {
            List<Path> mdFiles = stream
                    .filter(Files::isRegularFile)
                    .filter(p -> p.getFileName().toString().toLowerCase().endsWith(".md"))
                    .sorted(Comparator.comparing(p -> p.getFileName().toString()))
                    .toList();
            for (Path path : mdFiles) {
                String filename = path.getFileName().toString();
                Resource resource = new FileSystemResource(path.toFile());
                allDocuments.addAll(parseMarkdown(resource, filename));
            }
        } catch (IOException e) {
            log.error("Markdown 文档加载失败: {}", absolute, e);
        }
        return allDocuments;
    }

    private List<Document> parseMarkdown(Resource resource, String filename) throws IOException {
        String status = filename.length() >= 6
                ? filename.substring(filename.length() - 6, filename.length() - 4)
                : "";
        MarkdownDocumentReaderConfig config = MarkdownDocumentReaderConfig.builder()
                .withHorizontalRuleCreateDocument(true)
                .withIncludeCodeBlock(false)
                .withIncludeBlockquote(false)
                .withAdditionalMetadata("filename", filename)
                .withAdditionalMetadata("status", status)
                .build();
        MarkdownDocumentReader markdownDocumentReader = new MarkdownDocumentReader(resource, config);
        return markdownDocumentReader.get();
    }
}
