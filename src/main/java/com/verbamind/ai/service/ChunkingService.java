package com.verbamind.ai.service;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class ChunkingService {

    private static final int CHUNK_SIZE = 1000;   // characters
    private static final int CHUNK_OVERLAP = 150;  // characters

    public List<String> chunk(String text) {
        List<String> chunks = new ArrayList<>();
        if (text == null || text.isBlank()) return chunks;

        String normalized = text.replaceAll("\\s+", " ").trim();
        int length = normalized.length();
        int start = 0;

        while (start < length) {
            int end = Math.min(start + CHUNK_SIZE, length);
            chunks.add(normalized.substring(start, end));
            if (end == length) break;
            start = end - CHUNK_OVERLAP;
        }
        return chunks;
    }
}