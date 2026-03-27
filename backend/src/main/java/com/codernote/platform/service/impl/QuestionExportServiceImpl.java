package com.codernote.platform.service.impl;

import com.codernote.platform.common.PageResult;
import com.codernote.platform.dto.question.QuestionAttachmentVO;
import com.codernote.platform.dto.question.QuestionDetailVO;
import com.codernote.platform.dto.question.QuestionListItemVO;
import com.codernote.platform.exception.BizException;
import com.codernote.platform.service.QuestionExportService;
import com.codernote.platform.service.QuestionService;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class QuestionExportServiceImpl implements QuestionExportService {

    private static final long QUERY_PAGE_SIZE = 200L;
    private static final float PAGE_MARGIN = 44f;
    private static final float TITLE_FONT_SIZE = 16f;
    private static final float BODY_FONT_SIZE = 10.5f;
    private static final float LINE_HEIGHT = 15f;
    private static final float BLOCK_GAP = 6f;
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final QuestionService questionService;

    public QuestionExportServiceImpl(QuestionService questionService) {
        this.questionService = questionService;
    }

    @Override
    public byte[] exportPdf(Long userId, String language, String tag, String masteryStatus, String keyword) {
        List<QuestionDetailVO> questions = loadQuestions(userId, language, tag, masteryStatus, keyword);
        return buildPdf(questions, "筛选条件导出", buildFilterSummary(language, tag, masteryStatus, keyword));
    }

    @Override
    public byte[] exportPdfByQuestionId(Long userId, Long questionId) {
        QuestionDetailVO detail = questionService.detail(userId, questionId);
        return buildPdf(Collections.singletonList(detail), "单个错题导出", "错题ID=" + questionId);
    }

    @Override
    public byte[] exportPdfByQuestionIds(Long userId, List<Long> questionIds) {
        LinkedHashSet<Long> dedupIds = new LinkedHashSet<>();
        if (!CollectionUtils.isEmpty(questionIds)) {
            for (Long questionId : questionIds) {
                if (questionId != null) {
                    dedupIds.add(questionId);
                }
            }
        }

        List<QuestionDetailVO> questions = new ArrayList<>();
        for (Long questionId : dedupIds) {
            questions.add(questionService.detail(userId, questionId));
        }
        return buildPdf(questions, "选中错题导出", "选中数量=" + questions.size());
    }

    private byte[] buildPdf(List<QuestionDetailVO> questions, String exportType, String scope) {
        try (PDDocument document = new PDDocument();
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            PDFont font = loadFont(document);
            PdfCursor cursor = createPage(document);

            cursor = writeParagraph(document, cursor, font, TITLE_FONT_SIZE, PAGE_MARGIN, "CoderNote 错题导出");
            cursor = writeParagraph(document, cursor, font, BODY_FONT_SIZE, PAGE_MARGIN,
                    "导出时间: " + LocalDateTime.now().format(DATE_TIME_FORMATTER));
            cursor = writeParagraph(document, cursor, font, BODY_FONT_SIZE, PAGE_MARGIN,
                    "导出方式: " + (StringUtils.hasText(exportType) ? exportType : "筛选条件导出"));
            cursor = writeParagraph(document, cursor, font, BODY_FONT_SIZE, PAGE_MARGIN,
                    "导出范围: " + (StringUtils.hasText(scope) ? scope : "全部"));
            cursor.y -= BLOCK_GAP;

            if (CollectionUtils.isEmpty(questions)) {
                cursor = writeParagraph(document, cursor, font, BODY_FONT_SIZE, PAGE_MARGIN,
                        "暂无可导出的错题");
            } else {
                for (int i = 0; i < questions.size(); i++) {
                    QuestionDetailVO detail = questions.get(i);
                    cursor = writeQuestionBlock(document, cursor, font, i + 1, detail);
                }
            }

            cursor.close();
            document.save(outputStream);
            return outputStream.toByteArray();
        } catch (IOException ex) {
            throw new BizException(500, "Failed to export PDF");
        }
    }

    private List<QuestionDetailVO> loadQuestions(Long userId,
                                                 String language,
                                                 String tag,
                                                 String masteryStatus,
                                                 String keyword) {
        long pageNo = 1L;
        long total = 0L;
        List<QuestionDetailVO> questions = new ArrayList<>();

        while (true) {
            PageResult<QuestionListItemVO> page = questionService.page(
                    userId, pageNo, QUERY_PAGE_SIZE, language, tag, masteryStatus, keyword
            );

            if (page == null || CollectionUtils.isEmpty(page.getRecords())) {
                break;
            }

            total = page.getTotal() == null ? 0L : page.getTotal();
            for (QuestionListItemVO item : page.getRecords()) {
                if (item == null || item.getId() == null) {
                    continue;
                }
                questions.add(questionService.detail(userId, item.getId()));
            }

            if (questions.size() >= total) {
                break;
            }
            pageNo++;
        }

        return questions;
    }

    private String buildFilterSummary(String language, String tag, String masteryStatus, String keyword) {
        List<String> filters = new ArrayList<>();
        if (StringUtils.hasText(language)) {
            filters.add("语言=" + language.trim());
        }
        if (StringUtils.hasText(tag)) {
            filters.add("标签=" + tag.trim());
        }
        if (StringUtils.hasText(masteryStatus)) {
            filters.add("掌握状态=" + toMasteryLabel(masteryStatus.trim()));
        }
        if (StringUtils.hasText(keyword)) {
            filters.add("搜索=" + keyword.trim());
        }
        if (filters.isEmpty()) {
            return "全部";
        }
        return String.join("；", filters);
    }

    private String toMasteryLabel(String masteryStatus) {
        if (!StringUtils.hasText(masteryStatus)) {
            return "";
        }
        if ("MASTERED".equalsIgnoreCase(masteryStatus)) {
            return "已掌握";
        }
        if ("REVIEWING".equalsIgnoreCase(masteryStatus)) {
            return "复习中";
        }
        return "未复习";
    }

    private PdfCursor writeQuestionBlock(PDDocument document,
                                         PdfCursor cursor,
                                         PDFont font,
                                         int index,
                                         QuestionDetailVO detail) throws IOException {
        cursor = drawDivider(document, cursor);
        cursor = writeParagraph(document, cursor, font, BODY_FONT_SIZE, PAGE_MARGIN,
                "错题 #" + index);
        cursor = writeField(document, cursor, font, "标题", detail.getTitle());
        cursor = writeField(document, cursor, font, "编程语言", detail.getLanguage());
        cursor = writeField(document, cursor, font, "知识点标签", joinList(detail.getTagNames()));
        cursor = writeField(document, cursor, font, "掌握状态", toMasteryLabel(detail.getMasteryStatus()));
        cursor = writeField(document, cursor, font, "新增时间", formatDateTime(detail.getCreatedAt()));
        cursor = writeField(document, cursor, font, "更新时间", formatDateTime(detail.getUpdatedAt()));

        cursor = writeField(document, cursor, font, "错误题目", detail.getErrorCode());
        cursor = writeAttachments(document, cursor, font, "错误题目附件", detail.getErrorQuestionAttachments());
        cursor = writeField(document, cursor, font, "错误原因", detail.getErrorReason());
        cursor = writeField(document, cursor, font, "正确方案", detail.getCorrectCode());
        cursor = writeAttachments(document, cursor, font, "正确方案附件", detail.getCorrectSolutionAttachments());
        cursor = writeField(document, cursor, font, "解决方案", detail.getSolution());
        cursor = writeField(document, cursor, font, "错题来源", detail.getSource());
        cursor = writeField(document, cursor, font, "备注", detail.getRemark());

        cursor.y -= BLOCK_GAP;
        return cursor;
    }

    private PdfCursor writeField(PDDocument document,
                                 PdfCursor cursor,
                                 PDFont font,
                                 String label,
                                 String value) throws IOException {
        String safeValue = StringUtils.hasText(value) ? normalizeText(value) : "-";
        return writeParagraph(document, cursor, font, BODY_FONT_SIZE, PAGE_MARGIN, label + ": " + safeValue);
    }

    private PdfCursor writeAttachments(PDDocument document,
                                       PdfCursor cursor,
                                       PDFont font,
                                       String label,
                                       List<QuestionAttachmentVO> attachments) throws IOException {
        if (CollectionUtils.isEmpty(attachments)) {
            return writeParagraph(document, cursor, font, BODY_FONT_SIZE, PAGE_MARGIN, label + ": -");
        }
        String names = attachments.stream()
                .filter(Objects::nonNull)
                .map(item -> StringUtils.hasText(item.getFileName()) ? item.getFileName().trim() : item.getPath())
                .filter(StringUtils::hasText)
                .collect(Collectors.joining(", "));
        if (!StringUtils.hasText(names)) {
            names = "-";
        }
        return writeParagraph(document, cursor, font, BODY_FONT_SIZE, PAGE_MARGIN, label + ": " + names);
    }

    private PdfCursor drawDivider(PDDocument document, PdfCursor cursor) throws IOException {
        cursor = ensureSpace(document, cursor, LINE_HEIGHT);
        float maxX = cursor.page.getMediaBox().getWidth() - PAGE_MARGIN;
        cursor.contentStream.moveTo(PAGE_MARGIN, cursor.y);
        cursor.contentStream.lineTo(maxX, cursor.y);
        cursor.contentStream.stroke();
        cursor.y -= LINE_HEIGHT;
        return cursor;
    }

    private PdfCursor writeParagraph(PDDocument document,
                                     PdfCursor cursor,
                                     PDFont font,
                                     float fontSize,
                                     float startX,
                                     String text) throws IOException {
        float maxWidth = cursor.page.getMediaBox().getWidth() - PAGE_MARGIN - startX;
        List<String> lines = wrapText(font, fontSize, maxWidth, text);
        if (lines.isEmpty()) {
            lines = Collections.singletonList("");
        }
        for (String line : lines) {
            cursor = ensureSpace(document, cursor, LINE_HEIGHT);
            String drawText = safePdfText(line);
            cursor.contentStream.beginText();
            cursor.contentStream.setFont(font, fontSize);
            cursor.contentStream.newLineAtOffset(startX, cursor.y);
            try {
                cursor.contentStream.showText(drawText);
            } catch (IllegalArgumentException ex) {
                cursor.contentStream.showText(toAsciiFallback(drawText));
            }
            cursor.contentStream.endText();
            cursor.y -= LINE_HEIGHT;
        }
        return cursor;
    }

    private List<String> wrapText(PDFont font, float fontSize, float maxWidth, String text) throws IOException {
        String value = normalizeText(text);
        if (!StringUtils.hasText(value)) {
            return Collections.singletonList("");
        }
        List<String> output = new ArrayList<>();
        String[] paragraphs = value.split("\\n", -1);
        for (String paragraph : paragraphs) {
            if (paragraph.isEmpty()) {
                output.add("");
                continue;
            }
            StringBuilder line = new StringBuilder();
            for (int i = 0; i < paragraph.length(); i++) {
                char ch = paragraph.charAt(i);
                line.append(ch);
                if (textWidth(font, fontSize, line.toString()) > maxWidth) {
                    line.deleteCharAt(line.length() - 1);
                    if (line.length() > 0) {
                        output.add(line.toString());
                        line.setLength(0);
                        line.append(ch);
                    } else {
                        output.add(String.valueOf(ch));
                        line.setLength(0);
                    }
                }
            }
            output.add(line.toString());
        }
        return output;
    }

    private float textWidth(PDFont font, float fontSize, String text) throws IOException {
        try {
            return font.getStringWidth(text) / 1000f * fontSize;
        } catch (IllegalArgumentException ex) {
            return font.getStringWidth(toAsciiFallback(text)) / 1000f * fontSize;
        }
    }

    private String safePdfText(String text) {
        String value = text == null ? "" : text;
        return value
                .replace("\u0000", "")
                .replace("\t", "    ");
    }

    private String toAsciiFallback(String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }
        StringBuilder builder = new StringBuilder(text.length());
        for (int i = 0; i < text.length(); i++) {
            char ch = text.charAt(i);
            builder.append(ch <= 127 ? ch : '?');
        }
        return builder.toString();
    }

    private String normalizeText(String text) {
        if (text == null) {
            return "";
        }
        return text.replace("\r\n", "\n").replace('\r', '\n').trim();
    }

    private String joinList(List<String> list) {
        if (CollectionUtils.isEmpty(list)) {
            return "-";
        }
        String joined = list.stream()
                .filter(StringUtils::hasText)
                .map(String::trim)
                .collect(Collectors.joining(", "));
        return StringUtils.hasText(joined) ? joined : "-";
    }

    private String formatDateTime(LocalDateTime dateTime) {
        if (dateTime == null) {
            return "-";
        }
        return dateTime.format(DATE_TIME_FORMATTER);
    }

    private PDFont loadFont(PDDocument document) {
        List<Path> candidates = new ArrayList<>();
        candidates.add(Paths.get("C:/Windows/Fonts/msyh.ttf"));
        candidates.add(Paths.get("C:/Windows/Fonts/simhei.ttf"));
        candidates.add(Paths.get("C:/Windows/Fonts/simkai.ttf"));
        candidates.add(Paths.get("C:/Windows/Fonts/STKAITI.TTF"));
        candidates.add(Paths.get("C:/Windows/Fonts/arialuni.ttf"));

        for (Path path : candidates) {
            if (!Files.isRegularFile(path)) {
                continue;
            }
            try (InputStream in = Files.newInputStream(path)) {
                return PDType0Font.load(document, in, true);
            } catch (Exception ignore) {
                // Try the next candidate font.
            }
        }
        return PDType1Font.HELVETICA;
    }

    private PdfCursor ensureSpace(PDDocument document, PdfCursor cursor, float requiredHeight) throws IOException {
        if (cursor.y - requiredHeight < PAGE_MARGIN) {
            cursor.close();
            cursor = createPage(document);
        }
        return cursor;
    }

    private PdfCursor createPage(PDDocument document) throws IOException {
        PDPage page = new PDPage(PDRectangle.A4);
        document.addPage(page);
        PDPageContentStream stream = new PDPageContentStream(document, page);
        PdfCursor cursor = new PdfCursor();
        cursor.page = page;
        cursor.contentStream = stream;
        cursor.y = page.getMediaBox().getHeight() - PAGE_MARGIN;
        return cursor;
    }

    private static class PdfCursor {
        private PDPage page;
        private PDPageContentStream contentStream;
        private float y;

        private void close() throws IOException {
            if (contentStream != null) {
                contentStream.close();
                contentStream = null;
            }
        }
    }
}
