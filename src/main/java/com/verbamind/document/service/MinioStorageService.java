package com.verbamind.document.service;

import com.verbamind.config.MinioProperties;
import com.verbamind.document.exception.StorageException;
import io.minio.*;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;

import java.io.InputStream;

@Service
public class MinioStorageService implements StorageService {

    private final MinioClient minioClient;
    private final MinioProperties properties;

    public MinioStorageService(MinioClient minioClient, MinioProperties properties) {
        this.minioClient = minioClient;
        this.properties = properties;
    }

    @PostConstruct
    public void ensureBucketExists() {
        try {
            boolean exists = minioClient.bucketExists(
                    BucketExistsArgs.builder().bucket(properties.getBucket()).build());
            if (!exists) {
                minioClient.makeBucket(MakeBucketArgs.builder().bucket(properties.getBucket()).build());
            }
        } catch (Exception e) {
            throw new StorageException("Failed to initialize storage bucket: " + e.getMessage());
        }
    }

    @Override
    public void upload(String storageKey, InputStream stream, long size, String contentType) {
        try {
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(properties.getBucket())
                    .object(storageKey)
                    .stream(stream, size, -1L)
                    .contentType(contentType)
                    .build());
        } catch (Exception e) {
            throw new StorageException("Failed to upload file: " + e.getMessage());
        }
    }

    @Override
    public InputStream download(String storageKey) {
        try {
            return minioClient.getObject(GetObjectArgs.builder()
                    .bucket(properties.getBucket())
                    .object(storageKey)
                    .build());
        } catch (Exception e) {
            throw new StorageException("Failed to download file: " + e.getMessage());
        }
    }

    @Override
    public void delete(String storageKey) {
        try {
            minioClient.removeObject(RemoveObjectArgs.builder()
                    .bucket(properties.getBucket())
                    .object(storageKey)
                    .build());
        } catch (Exception e) {
            throw new StorageException("Failed to delete file: " + e.getMessage());
        }
    }
}