package com.verbamind.document.service;

import java.io.InputStream;

public interface StorageService {
    void upload(String storageKey, InputStream stream, long size, String contentType);
    InputStream download(String storageKey);
    void delete(String storageKey);
}