package com.fastbills.api.storage;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

@Service
public class LocalStorageService implements StorageService {

    private final Path rootLocation;

    public LocalStorageService(@Value("${fastbills.storage.local-dir:./data/storage}") String storageDir) {
        this.rootLocation = Paths.get(storageDir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.rootLocation);
        } catch (IOException e) {
            throw new IllegalStateException("Could not initialize storage directory at " + rootLocation, e);
        }
    }

    @Override
    public String store(String objectKey, InputStream content, String contentType, long size) {
        try {
            Path destinationFile = resolveSafePath(objectKey);
            Path parentDir = destinationFile.getParent();
            if (parentDir != null) {
                Files.createDirectories(parentDir);
            }
            Files.copy(content, destinationFile, StandardCopyOption.REPLACE_EXISTING);
            return objectKey;
        } catch (IOException e) {
            throw new RuntimeException("Failed to store file: " + objectKey, e);
        }
    }

    @Override
    public Resource load(String objectKey) {
        try {
            Path file = resolveSafePath(objectKey);
            Resource resource = new UrlResource(file.toUri());
            if (resource.exists() && resource.isReadable()) {
                return resource;
            } else {
                throw new IllegalArgumentException("File not found: " + objectKey);
            }
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException("Invalid file path: " + objectKey, e);
        }
    }

    @Override
    public void delete(String objectKey) {
        try {
            Path file = resolveSafePath(objectKey);
            Files.deleteIfExists(file);
        } catch (IOException e) {
            throw new RuntimeException("Failed to delete file: " + objectKey, e);
        }
    }

    private Path resolveSafePath(String objectKey) {
        Path resolved = rootLocation.resolve(objectKey).normalize();
        if (!resolved.startsWith(rootLocation)) {
            throw new SecurityException("Cannot store or read files outside root directory: " + objectKey);
        }
        return resolved;
    }
}
