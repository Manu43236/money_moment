package com.moneymoment.lending.services;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@Service
public class R2StorageService {

    private final Cloudinary cloudinary;

    R2StorageService(
            @Value("${cloudinary.cloud-name}") String cloudName,
            @Value("${cloudinary.api-key}") String apiKey,
            @Value("${cloudinary.api-secret}") String apiSecret) {

        this.cloudinary = new Cloudinary(ObjectUtils.asMap(
                "cloud_name", cloudName,
                "api_key", apiKey,
                "api_secret", apiSecret,
                "secure", true));
    }

    /**
     * Uploads a file to Cloudinary.
     * The key is used as the public_id so it can be used for deletion later.
     *
     * @param key  used as Cloudinary public_id (e.g. loans/LN2026.../PAN_CARD/uuid)
     * @param file the multipart file to upload
     * @return secure HTTPS URL of the uploaded file
     */
    public String upload(String key, MultipartFile file) throws IOException {
        Map<?, ?> result = cloudinary.uploader().upload(file.getBytes(), ObjectUtils.asMap(
                "public_id", key,
                "resource_type", "auto",
                "overwrite", false));

        return (String) result.get("secure_url");
    }

    /**
     * Deletes a file from Cloudinary by its public_id (stored as filePath in DB).
     *
     * @param publicId Cloudinary public_id (same as key used during upload)
     */
    public void delete(String publicId) throws IOException {
        cloudinary.uploader().destroy(publicId, ObjectUtils.asMap("resource_type", "raw"));
    }
}
