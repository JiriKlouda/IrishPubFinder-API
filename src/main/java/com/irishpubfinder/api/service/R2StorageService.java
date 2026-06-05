package com.irishpubfinder.api.service;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.net.URI;

@Slf4j
@Service
public class R2StorageService {

    private static final String PREFIX = "photos/";

    @Value("${r2.account.id}")
    private String accountId;

    @Value("${r2.access.key.id}")
    private String accessKeyId;

    @Value("${r2.secret.access.key}")
    private String secretAccessKey;

    @Value("${r2.bucket.name}")
    private String bucketName;

    @Value("${r2.public.url}")
    private String publicBaseUrl;

    private S3Client s3;

    @PostConstruct
    void init() {
        if (accountId.isBlank() || accessKeyId.isBlank() || secretAccessKey.isBlank()) {
            log.warn("R2 credentials not configured — photo proxy will return 503 until credentials are set");
            return;
        }
        String endpoint = "https://" + accountId + ".r2.cloudflarestorage.com";
        s3 = S3Client.builder()
                .endpointOverride(URI.create(endpoint))
                .region(Region.of("auto"))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(accessKeyId, secretAccessKey)))
                .serviceConfiguration(S3Configuration.builder()
                        .pathStyleAccessEnabled(true)
                        .build())
                .build();
        log.info("R2StorageService ready, bucket='{}'", bucketName);
    }

    public boolean isConfigured() {
        return s3 != null;
    }

    /** Returns true if the photo already exists in R2 (lightweight HEAD request). */
    public boolean exists(String photoRef) {
        try {
            s3.headObject(HeadObjectRequest.builder()
                    .bucket(bucketName)
                    .key(PREFIX + photoRef)
                    .build());
            return true;
        } catch (NoSuchKeyException e) {
            return false;
        }
    }

    /** Uploads photo bytes to R2. Concurrent uploads of the same ref are idempotent. */
    public void upload(String photoRef, byte[] bytes, String contentType) {
        s3.putObject(
                PutObjectRequest.builder()
                        .bucket(bucketName)
                        .key(PREFIX + photoRef)
                        .contentType(contentType)
                        .contentLength((long) bytes.length)
                        .build(),
                RequestBody.fromBytes(bytes));
        log.info("Uploaded photo to R2: {}{}", PREFIX, photoRef);
    }

    /** Returns the public CDN URL for a stored photo. */
    public String publicUrl(String photoRef) {
        return publicBaseUrl.stripTrailing() + "/" + PREFIX + photoRef;
    }
}
