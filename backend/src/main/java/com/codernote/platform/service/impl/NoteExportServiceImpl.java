package com.codernote.platform.service.impl;

import com.codernote.platform.common.PageResult;
import com.codernote.platform.dto.note.NoteDetailVO;
import com.codernote.platform.dto.note.NoteListItemVO;
import com.codernote.platform.exception.BizException;
import com.codernote.platform.service.NoteExportService;
import com.codernote.platform.service.NoteService;
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
public class NoteExportServiceImpl implements NoteExportService {

    private static final long QUERY_PAGE_SIZE = 200L;
    private static final float PAGE_MARGIN = 44f;
    private static final float TITLE_FONT_SIZE = 16f;
    private static final float BODY_FONT_SIZE = 10.5f;
    private static final float LINE_HEIGHT = 15f;
    private static final float BLOCK_GAP = 6f;
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final NoteService noteService;

    public NoteExportServiceImpl(NoteService noteService) {
        this.noteService = noteService;
    }

    @Override
    public byte[] exportPdf(Long userId,
                            String language,
                            String tag,
                            String favoriteStatus,
                            String keyword,
                            String sortBy,
                            String sortOrder) {
        List<NoteDetailVO> notes = loadNotes(userId, language, tag, favoriteStatus, keyword, sortBy, sortOrder);
        return buildPdf(notes, "筛选条件导出", buildFilterSummary(language, tag, favoriteStatus, keyword, sortBy, sortOrder));
    }

    @Override
    public byte[] exportPdfByNoteId(Long userId, Long noteId) {
        NoteDetailVO detail = noteService.detail(userId, noteId);
        return buildPdf(Collections.singletonList(detail), "单个笔记导出", "笔记ID=" + noteId);
    }

    @Override
    public byte[] exportPdfByNoteIds(Long userId, List<Long> noteIds) {
        LinkedHashSet<Long> dedupIds = new LinkedHashSet<>();
        if (!CollectionUtils.isEmpty(noteIds)) {
            for (Long noteId : noteIds) {
                if (noteId != null) {
                    dedupIds.add(noteId);
                }
            }
        }

        List<NoteDetailVO> notes = new ArrayList<>();
        for (Long noteId : dedupIds) {
            notes.add(noteService.detail(userId, noteId));
        }
        return buildPdf(notes, "选中笔记导出", "选中数量=" + notes.size());
    }

    private byte[] buildPdf(List<NoteDetailVO> notes, String exportType, String scope) {
        try (PDDocument document = new PDDocument();
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            PDFont font = loadFont(document);
            PdfCursor cursor = createPage(document);

            cursor = writeParagraph(document, cursor, font, TITLE_FONT_SIZE, PAGE_MARGIN, "CoderNote 笔记导出");
            cursor = writeParagraph(document, cursor, font, BODY_FONT_SIZE, PAGE_MARGIN,
                    "导出时间: " + LocalDateTime.now().format(DATE_TIME_FORMATTER));
            cursor = writeParagraph(document, cursor, font, BODY_FONT_SIZE, PAGE_MARGIN,
                    "导出方式: " + (StringUtils.hasText(exportType) ? exportType : "筛选条件导出"));
            cursor = writeParagraph(document, cursor, font, BODY_FONT_SIZE, PAGE_MARGIN,
                    "导出范围: " + (StringUtils.hasText(scope) ? scope : "全部"));
            cursor.y -= BLOCK_GAP;

            if (CollectionUtils.isEmpty(notes)) {
                cursor = writeParagraph(document, cursor, font, BODY_FONT_SIZE, PAGE_MARGIN,
                        "暂无可导出的笔记");
            } else {
                for (int i = 0; i < notes.size(); i++) {
                    cursor = writeNoteBlock(document, cursor, font, i + 1, notes.get(i));
                }
            }

            cursor.close();
            document.save(outputStream);
            return outputStream.toByteArray();
        } catch (IOException ex) {
            throw new BizException(500, "Failed to export PDF");
        }
    }

    private List<NoteDetailVO> loadNotes(Long userId,
                                         String language,
                                         String tag,
                                         String favoriteStatus,
                                         String keyword,
                                         String sortBy,
                                         String sortOrder) {
        long pageNo = 1L;
        long total = 0L;
        List<NoteDetailVO> notes = new ArrayList<>();

        while (true) {
            PageResult<NoteListItemVO> page = noteService.page(
                    userId, pageNo, QUERY_PAGE_SIZE, language, tag, favoriteStatus, keyword, sortBy, sortOrder
            );

            if (page == null || CollectionUtils.isEmpty(page.getRecords())) {
                break;
            }

            total = page.getTotal() == null ? 0L : page.getTotal();
            for (NoteListItemVO item : page.getRecords()) {
                if (item == null || item.getId() == null) {
                    continue;
                }
                notes.add(noteService.detail(userId, item.getId()));
            }

            if (notes.size() >= total) {
                break;
            }
            pageNo++;
        }

        return notes;
    }

    private String buildFilterSummary(String language,
                                      String tag,
                                      String favoriteStatus,
                                      String keyword,
                                      String sortBy,
                                      String sortOrder) {
        List<String> filters = new ArrayList<>();
        if (StringUtils.hasText(language)) {
            filters.add("语言=" + language.trim());
        }
        if (StringUtils.hasText(tag)) {
            filters.add("标签=" + tag.trim());
        }
        if (StringUtils.hasText(favoriteStatus)) {
            filters.add("收藏状态=" + toFavoriteLabel(favoriteStatus.trim()));
        }
        if (StringUtils.hasText(keyword)) {
            filters.add("搜索=" + keyword.trim());
        }
        if (StringUtils.hasText(sortBy)) {
            filters.add("排序字段=" + toSortByLabel(sortBy.trim()));
        }
        if (StringUtils.hasText(sortOrder)) {
            filters.add("排序方式=" + toSortOrderLabel(sortOrder.trim()));
        }

        if (filters.isEmpty()) {
            return "全部";
        }
        return String.join("；", filters);
    }

    private String toFavoriteLabel(String favoriteStatus) {
        if ("FAVORITE".equalsIgnoreCase(favoriteStatus)) {
            return "已收藏";
        }
        if ("UNFAVORITE".equalsIgnoreCase(favoriteStatus)) {
            return "未收藏";
        }
        return favoriteStatus;
    }

    private String toSortByLabel(String sortBy) {
        if ("created_at".equalsIgnoreCase(sortBy) || "createdAt".equalsIgnoreCase(sortBy)) {
            return "创建时间";
        }
        if ("updated_at".equalsIgnoreCase(sortBy) || "updatedAt".equalsIgnoreCase(sortBy)) {
            return "更新时间";
        }
        return sortBy;
    }

    private String toSortOrderLabel(String sortOrder) {
        if ("asc".equalsIgnoreCase(sortOrder)) {
            return "升序";
        }
        if ("desc".equalsIgnoreCase(sortOrder)) {
            return "降序";
        }
        return sortOrder;
    }

    private PdfCursor writeNoteBlock(PDDocument document,
                                     PdfCursor cursor,
                                     PDFont font,
                                     int index,
                                     NoteDetailVO detail) throws IOException {
        cursor = drawDivider(document, cursor);
        cursor = writeParagraph(document, cursor, font, BODY_FONT_SIZE, PAGE_MARGIN,
                "笔记 #" + index);

        cursor = writeField(document, cursor, font, "标题", detail.getTitle());
        cursor = writeField(document, cursor, font, "编程语言", detail.getLanguage());
        cursor = writeField(document, cursor, font, "知识点标签", joinList(detail.getTagNames()));
        cursor = writeField(document, cursor, font, "创建时间", formatDateTime(detail.getCreatedAt()));
        cursor = writeField(document, cursor, font, "更新时间", formatDateTime(detail.getUpdatedAt()));
        cursor = writeField(document, cursor, font, "收藏状态", Boolean.TRUE.equals(detail.getFavorite()) ? "已收藏" : "未收藏");
        cursor = writeField(document, cursor, font, "关联错题", joinRelatedQuestionTitles(detail.getRelatedQuestions()));
        cursor = writeField(document, cursor, font, "关联资料", joinRelatedMaterialTitles(detail.getRelatedMaterials()));
        cursor = writeField(document, cursor, font, "笔记内容", detail.getContent());

        cursor.y -= BLOCK_GAP;
        return cursor;
    }

    private String joinRelatedQuestionTitles(List<NoteDetailVO.LinkedQuestionVO> list) {
        if (CollectionUtils.isEmpty(list)) {
            return "-";
        }
        String value = list.stream()
                .filter(Objects::nonNull)
                .map(item -> StringUtils.hasText(item.getTitle()) ? item.getTitle().trim() : "#" + item.getId())
                .filter(StringUtils::hasText)
                .collect(Collectors.joining("、"));
        return StringUtils.hasText(value) ? value : "-";
    }

    private String joinRelatedMaterialTitles(List<NoteDetailVO.LinkedMaterialVO> list) {
        if (CollectionUtils.isEmpty(list)) {
            return "-";
        }
        String value = list.stream()
                .filter(Objects::nonNull)
                .map(item -> StringUtils.hasText(item.getTitle()) ? item.getTitle().trim() : "#" + item.getId())
                .filter(StringUtils::hasText)
                .collect(Collectors.joining("、"));
        return StringUtils.hasText(value) ? value : "-";
    }

    private PdfCursor writeField(PDDocument document,
                                 PdfCursor cursor,
                                 PDFont font,
                                 String label,
                                 String value) throws IOException {
        String safeValue = StringUtils.hasText(value) ? normalizeText(value) : "-";
        return writeParagraph(document, cursor, font, BODY_FONT_SIZE, PAGE_MARGIN, label + ": " + safeValue);
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
                // Try next candidate font.
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
