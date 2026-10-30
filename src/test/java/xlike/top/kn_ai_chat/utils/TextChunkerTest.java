package xlike.top.kn_ai_chat.utils;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class TextChunkerTest {

    @Test
    void chunksLongTextWithOverlap() {
        StringBuilder text = new StringBuilder();
        for (int i = 0; i < 100; i++) {
            text.append("这是第").append(i).append("段企业产品资料。");
        }
        List<String> chunks = TextChunker.chunk(text.toString(), 100, 20);
        assertThat(chunks).hasSizeGreaterThan(1);
        assertThat(chunks).allSatisfy(chunk -> assertThat(chunk.length()).isLessThanOrEqualTo(100));
    }

    @Test
    void blankTextReturnsEmpty() {
        assertThat(TextChunker.chunk("   ", 100, 20)).isEmpty();
    }
}
