package com.fastbills.api.storage;

import org.springframework.core.io.Resource;
import java.io.InputStream;

public interface StorageService {
    /**
     * Stores an object and returns the object key.
     */
    String store(String objectKey, InputStream content, String contentType, long size);

    /**
     * Loads an object as a Spring Resource.
     */
    Resource load(String objectKey);

    /**
     * Deletes an object if it exists.
     */
    void delete(String objectKey);
}
