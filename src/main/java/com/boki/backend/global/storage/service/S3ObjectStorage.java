package com.boki.backend.global.storage.service;

public interface S3ObjectStorage {

    void delete(String bucket, String objectKey);
}
