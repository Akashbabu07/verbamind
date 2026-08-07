package com.verbamind.document.service;

import com.verbamind.config.MinioProperties;
import com.verbamind.document.exception.StorageException;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.InputStream;

@Service
public class MinioStorageService implements StorageService {

    private final S3Client s3Client;
    private final MinioProperties properties;

    public MinioStorageService(
            S3Client s3Client,
            MinioProperties properties
    ) {
        this.s3Client = s3Client;
        this.properties = properties;
    }

    @Override
    public void upload(
            String storageKey,
            InputStream stream,
            long size,
            String contentType
    ) {
        try {
            PutObjectRequest request = PutObjectRequest.builder()
                    .bucket(properties.getBucket())
                    .key(storageKey)
                    .contentType(contentType)
                    .build();

            s3Client.putObject(
                    request,
                    RequestBody.fromInputStream(stream, size)
            );

        } catch (Exception e) {
            throw new StorageException(
                    "Failed to upload file: " + e.getMessage()
            );
        }
    }

    @Override
    public InputStream download(String storageKey) {
        try {
            GetObjectRequest request = GetObjectRequest.builder()
                    .bucket(properties.getBucket())
                    .key(storageKey)
                    .build();

            ResponseInputStream<GetObjectResponse> response =
                    s3Client.getObject(request);

            return response;

        } catch (Exception e) {
            throw new StorageException(
                    "Failed to download file: " + e.getMessage()
            );
        }
    }

    @Override
    public void delete(String storageKey) {
        try {
            DeleteObjectRequest request = DeleteObjectRequest.builder()
                    .bucket(properties.getBucket())
                    .key(storageKey)
                    .build();

            s3Client.deleteObject(request);

        } catch (Exception e) {
            throw new StorageException(
                    "Failed to delete file: " + e.getMessage()
            );
        }
    }
}