package xlike.top.kn_ai_chat.utils;

import java.util.ArrayList;
import java.util.List;

/**
 * 简单的按字符切片器：适合中文资料，避免引入额外分词/向量框架。
 */
public final class TextChunker {

    private TextChunker() {
    }

    public static List<String> chunk(String text, int maxChars, int overlapChars) {
        if (text == null || text.isBlank()) {
            return List.of();
        }
        String normalized = text.replace("\r\n", "\n").replace('\r', '\n').trim();
        if (normalized.length() <= maxChars) {
            return List.of(normalized);
        }

        List<String> chunks = new ArrayList<>();
        int start = 0;
        while (start < normalized.length()) {
            int end = Math.min(start + maxChars, normalized.length());
            // 尽量在段落/句号处断开，避免切断词语
            if (end < normalized.length()) {
                int breakAt = Math.max(
                        normalized.lastIndexOf('\n', end),
                        Math.max(normalized.lastIndexOf('。', end), normalized.lastIndexOf('；', end))
                );
                if (breakAt > start + maxChars / 2) {
                    end = breakAt + 1;
                }
            }
            String chunk = normalized.substring(start, end).trim();
            if (!chunk.isEmpty()) {
                chunks.add(chunk);
            }
            if (end >= normalized.length()) {
                break;
            }
            start = Math.max(end - overlapChars, start + 1);
        }
        return chunks;
    }
}
