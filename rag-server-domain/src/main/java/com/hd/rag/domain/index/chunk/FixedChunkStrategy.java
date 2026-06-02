package com.hd.rag.domain.index.chunk;

import com.hd.rag.domain.index.parse.TextSegment;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class FixedChunkStrategy implements ChunkStrategy {
    @Override
    public ChunkStrategyType getType() {
        return ChunkStrategyType.FIXED;
    }

    @Override
    public List<TextChunk> chunk(List<TextSegment> textSegmentList, ChunkOptions chunkOptions) {
        Objects.requireNonNull(textSegmentList, "textSegmentList must not be null");
        int chunkSize = Objects.requireNonNull(chunkOptions.getChunkSize(), "chunkSize must not be null");
        int overlapSize = chunkOptions.getOverlapSize() != null ? chunkOptions.getOverlapSize() : 0;

        List<TextChunk> result = new ArrayList<>();
        List<TextSegment> currentGroup = new ArrayList<>();
        int currentLength = 0;

        for (TextSegment seg : textSegmentList) {
            int segLength = seg.getText() == null ? 0 : seg.getText().length();
            if (segLength == 0) continue;

            if (!currentGroup.isEmpty() && currentLength + segLength > chunkSize) {
                result.add(buildChunk(currentGroup));
                currentGroup = overlapSegments(currentGroup, overlapSize);
                currentLength = currentGroup.stream()
                        .mapToInt(s -> s.getText().length())
                        .sum();
            }
            currentGroup.add(seg);
            currentLength += segLength;
        }
        if (!currentGroup.isEmpty()) {
            result.add(buildChunk(currentGroup));
        }
        return result;
    }

    private TextChunk buildChunk(List<TextSegment> segments) {
        String content = segments.stream()
                .map(TextSegment::getText)
                .collect(Collectors.joining("\n\n"));
        List<String> pointers = segments.stream()
                .map(TextSegment::getSourcePointer)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        return TextChunk.builder()
                .content(content)
                .sourcePointers(pointers)
                .build();
    }

    private List<TextSegment> overlapSegments(List<TextSegment> segments, int overlapSize) {
        if (overlapSize <= 0 || segments.isEmpty()) return new ArrayList<>();
        List<TextSegment> overlap = new ArrayList<>();
        int collected = 0;
        for (int i = segments.size() - 1; i >= 0 && collected < overlapSize; i--) {
            overlap.add(0, segments.get(i));
            collected += segments.get(i).getText().length();
        }
        return overlap;
    }
}
