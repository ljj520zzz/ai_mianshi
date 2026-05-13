package com.ljj.aiagent.tools;

import cn.hutool.core.io.FileUtil;
import com.itextpdf.io.font.PdfEncodings;
import com.itextpdf.io.font.constants.StandardFonts;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Text;
import com.itextpdf.layout.element.Table;
import com.ljj.aiagent.constant.FileConstant;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * PDF 生成工具
 */
public class PDFGenerationTool {
    private static final Pattern BOLD_PATTERN = Pattern.compile("\\*\\*(.+?)\\*\\*");
    private static final Pattern TABLE_SEPARATOR_PATTERN = Pattern.compile("^\\|?\\s*:?-{3,}:?\\s*(\\|\\s*:?-{3,}:?\\s*)+\\|?$");
    private PdfFont currentFont;

    @Tool(description = "Generate a PDF file with given content", returnDirect = false)
    public String generatePDF(
            @ToolParam(description = "Name of the file to save the generated PDF") String fileName,
            @ToolParam(description = "Content to be included in the PDF") String content) {
        String fileDir = FileConstant.FILE_SAVE_DIR + "/pdf";
        String filePath = fileDir + "/" + fileName;
        try {
            // 创建目录
            FileUtil.mkdir(fileDir);
            // 创建 PdfWriter 和 PdfDocument 对象
            try (PdfWriter writer = new PdfWriter(filePath);
                 PdfDocument pdf = new PdfDocument(writer);
                 Document document = new Document(pdf)) {
                PdfFont font = resolvePdfFont();
                currentFont = font;
                document.setFont(font);
                renderMarkdownContent(document, content);
            }
            return fileName;
        } catch (Exception e) {
            return "Error generating PDF: " + e.getMessage();
        }
    }

    private void renderMarkdownContent(Document document, String content) {
        if (content == null || content.isBlank()) {
            document.add(new Paragraph(""));
            return;
        }
        String normalized = normalizePdfContent(content).replace("\r\n", "\n");
        String[] lines = normalized.split("\n", -1);
        for (int i = 0; i < lines.length; i++) {
            String rawLine = lines[i];
            String line = rawLine == null ? "" : rawLine;
            String trimmed = line.trim();
            if (trimmed.isEmpty()) {
                document.add(new Paragraph(""));
                continue;
            }
            if (isTableLine(trimmed)) {
                int nextIndex = renderMarkdownTable(document, lines, i);
                if (nextIndex > i) {
                    i = nextIndex - 1;
                    continue;
                }
            }
            Paragraph paragraph = createParagraphByMarkdownLine(trimmed);
            document.add(paragraph);
        }
    }

    private String normalizePdfContent(String content) {
        if (content == null) {
            return "";
        }
        return content
                .replace('\u00A0', ' ')
                .replace('\u200B', ' ')
                .replace('\uFEFF', ' ')
                .replace("①", "1.")
                .replace("②", "2.")
                .replace("③", "3.")
                .replace("④", "4.")
                .replace("⑤", "5.")
                .replace("⑥", "6.")
                .replace("⑦", "7.")
                .replace("⑧", "8.")
                .replace("⑨", "9.")
                .replace("⑩", "10.")
                .replace("→", "->")
                .replace("←", "<-")
                .replace("≤", "<=")
                .replace("≥", ">=")
                .replace("≠", "!=")
                .replace("×", "x")
                .replace("✓", "v")
                .replace("√", "v")
                .replace("•", "-")
                .replace("●", "-")
                .replace("·", "-")
                .replace("—", "-")
                .replace("–", "-")
                .replace("“", "\"")
                .replace("”", "\"")
                .replace("‘", "'")
                .replace("’", "'")
                .replaceAll("[\\x{1F300}-\\x{1FAFF}]", "");
    }

    private String sanitizeForFont(String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }
        PdfFont font = currentFont;
        if (font == null) {
            return text;
        }
        StringBuilder builder = new StringBuilder(text.length());
        for (int index = 0; index < text.length(); ) {
            int codePoint = text.codePointAt(index);
            if (font.containsGlyph(codePoint)) {
                builder.appendCodePoint(codePoint);
            }
            index += Character.charCount(codePoint);
        }
        return builder.toString();
    }

    private Text createText(String content) {
        String safeContent = sanitizeForFont(normalizePdfContent(content));
        if (safeContent.isEmpty()) {
            return new Text("");
        }
        return new Text(safeContent);
    }

    private int renderMarkdownTable(Document document, String[] lines, int startIndex) {
        List<String> tableLines = new ArrayList<>();
        int index = startIndex;
        while (index < lines.length) {
            String current = lines[index] == null ? "" : lines[index].trim();
            if (!isTableLine(current)) {
                break;
            }
            tableLines.add(current);
            index++;
        }
        if (tableLines.size() < 2) {
            return startIndex;
        }
        if (!isTableSeparatorLine(tableLines.get(1))) {
            return startIndex;
        }
        List<String> headerCells = splitTableCells(tableLines.get(0));
        if (headerCells.isEmpty()) {
            return startIndex;
        }
        int colCount = headerCells.size();
        Table table = new Table(colCount);
        for (String header : headerCells) {
            table.addHeaderCell(buildInlineStyledParagraph(header));
        }
        for (int i = 2; i < tableLines.size(); i++) {
            String tableLine = tableLines.get(i);
            if (isTableSeparatorLine(tableLine)) {
                continue;
            }
            List<String> rowCells = splitTableCells(tableLine);
            for (int col = 0; col < colCount; col++) {
                String cellText = col < rowCells.size() ? rowCells.get(col) : "";
                table.addCell(buildInlineStyledParagraph(cellText));
            }
        }
        document.add(table);
        return index;
    }

    private boolean isTableLine(String line) {
        if (line == null || line.isBlank()) {
            return false;
        }
        return line.contains("|");
    }

    private boolean isTableSeparatorLine(String line) {
        if (line == null || line.isBlank()) {
            return false;
        }
        return TABLE_SEPARATOR_PATTERN.matcher(line).matches();
    }

    private List<String> splitTableCells(String line) {
        String normalized = line == null ? "" : line.trim();
        if (normalized.startsWith("|")) {
            normalized = normalized.substring(1);
        }
        if (normalized.endsWith("|")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        String[] parts = normalized.split("\\|", -1);
        List<String> cells = new ArrayList<>();
        for (String part : parts) {
            cells.add(part == null ? "" : part.trim());
        }
        return cells;
    }

    private Paragraph createParagraphByMarkdownLine(String line) {
        if (line.startsWith("### ")) {
            Paragraph paragraph = buildInlineStyledParagraph(line.substring(4).trim());
            paragraph.setFontSize(14);
            return paragraph;
        }
        if (line.startsWith("## ")) {
            Paragraph paragraph = buildInlineStyledParagraph(line.substring(3).trim());
            paragraph.setFontSize(16);
            return paragraph;
        }
        if (line.startsWith("# ")) {
            Paragraph paragraph = buildInlineStyledParagraph(line.substring(2).trim());
            paragraph.setFontSize(18);
            return paragraph;
        }
        if (line.startsWith("- ")) {
            return buildInlineStyledParagraph("- " + line.substring(2).trim());
        }
        if (line.matches("^\\d+\\.\\s+.*$")) {
            return buildInlineStyledParagraph(line);
        }
        return buildInlineStyledParagraph(line);
    }

    private Paragraph buildInlineStyledParagraph(String content) {
        String normalizedContent = content == null ? "" : content.replace("\\*\\*", "**");
        Paragraph paragraph = new Paragraph();
        Matcher matcher = BOLD_PATTERN.matcher(normalizedContent);
        int cursor = 0;
        while (matcher.find()) {
            if (matcher.start() > cursor) {
                paragraph.add(createText(normalizedContent.substring(cursor, matcher.start())));
            }
            paragraph.add(createText(matcher.group(1)));
            cursor = matcher.end();
        }
        if (cursor < normalizedContent.length()) {
            paragraph.add(createText(normalizedContent.substring(cursor)));
        }
        if (paragraph.isEmpty()) {
            paragraph.add(createText(normalizedContent.replace("**", "")));
        }
        return paragraph;
    }

    private PdfFont resolvePdfFont() throws IOException {
        List<String> builtinFonts = List.of("STSongStd-Light", "STSong-Light", "MHei-Medium");
        for (String fontName : builtinFonts) {
            try {
                return PdfFontFactory.createFont(
                        fontName,
                        "UniGB-UCS2-H",
                        PdfFontFactory.EmbeddingStrategy.PREFER_EMBEDDED
                );
            } catch (Exception ignored) {
                // 尝试下一个内置字体
            }
        }
        List<String> fontCandidates = List.of(
                "/usr/share/fonts/opentype/noto/NotoSansCJK-Regular.ttc,0",
                "/usr/share/fonts/truetype/wqy/wqy-microhei.ttc,0",
                "/usr/share/fonts/wqy-microhei/wqy-microhei.ttc,0",
                "/usr/share/fonts/truetype/arphic/uming.ttc,0",
                "C:/Windows/Fonts/msyh.ttc,0",
                "C:/Windows/Fonts/simsun.ttc,0",
                "C:/Windows/Fonts/msyh.ttc",
                "C:/Windows/Fonts/msyh.ttf",
                "C:/Windows/Fonts/simsun.ttc",
                "C:/Windows/Fonts/simsun.ttf",
                "C:/Windows/Fonts/simhei.ttf"
        );
        for (String fontPath : fontCandidates) {
            String realPath = fontPath.contains(",") ? fontPath.substring(0, fontPath.indexOf(',')) : fontPath;
            if (!Files.exists(Path.of(realPath))) {
                continue;
            }
            try {
                return PdfFontFactory.createFont(
                        fontPath,
                        PdfEncodings.IDENTITY_H,
                        PdfFontFactory.EmbeddingStrategy.PREFER_EMBEDDED
                );
            } catch (Exception ignored) {
                // 尝试下一个候选字体
            }
        }
        return PdfFontFactory.createFont(StandardFonts.HELVETICA);
    }
}
