package com.verbamind.ai.service;

import com.verbamind.ai.exception.AiProviderException;
import org.apache.tika.Tika;
import org.springframework.stereotype.Service;

import java.io.InputStream;

@Service
public class TextExtractionService {

    private final Tika tika = new Tika();

    public String extract(InputStream fileStream) {
        try {
            return tika.parseToString(fileStream);
        } catch (Exception e) {
            throw new AiProviderException("Text extraction failed: " + e.getMessage());
        }
    }
}