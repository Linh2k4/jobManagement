package com.company.jobmanagement.storage.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Binds to the {@code minio.*} keys already defined in application-local.yml /
 * application-prod.yml (endpoint, access-key, secret-key, bucket-name) — not a
 * separate {@code app.storage.minio.*} namespace those files never set.
 */
@ConfigurationProperties(prefix = "minio")
public class MinioProperties {
    private String endpoint = "http://localhost:9000";
    private String accessKey = "minioadmin";
    private String secretKey = "minioadmin";
    private String bucketName = "job-management";

    public String getEndpoint() { return endpoint; }
    public void setEndpoint(String endpoint) { this.endpoint = endpoint; }

    public String getAccessKey() { return accessKey; }
    public void setAccessKey(String accessKey) { this.accessKey = accessKey; }

    public String getSecretKey() { return secretKey; }
    public void setSecretKey(String secretKey) { this.secretKey = secretKey; }

    public String getBucketName() { return bucketName; }
    public void setBucketName(String bucketName) { this.bucketName = bucketName; }
}
