package ecom.media_service.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import ecom.media_service.dto.response.MediaImageResponse;
import ecom.media_service.dto.response.ProductImagesResponse;
import ecom.media_service.dto.response.ProfileImageResponse;
import ecom.media_service.exception.BadRequestException;
import ecom.media_service.exception.MediaNotFoundException;
import ecom.media_service.model.Media;
import ecom.media_service.model.OwnerType;
import ecom.media_service.repository.MediaRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MediaService {

    private static final int MAX_FILES_PER_PRODUCT_UPLOAD = 5;

    private final MediaRepository mediaRepository;
    private final MediaStorageService mediaStorageService;
    private final FileValidationService fileValidationService;
    private final MediaUrlBuilder mediaUrlBuilder;
    private final ProductOwnershipService productOwnershipService;

    public ProductImagesResponse uploadProductImages(String uploaderId, String productId, MultipartFile[] files) {
        requireNonBlank(uploaderId, "Authenticated user is required");
        requireNonBlank(productId, "productId is required");

        if (files == null || files.length == 0) {
            throw new BadRequestException("At least one image file is required");
        }
        if (files.length > MAX_FILES_PER_PRODUCT_UPLOAD) {
            throw new BadRequestException("A maximum of 5 image files is allowed per upload");
        }

        productOwnershipService.verifyOwnership(productId, uploaderId);
        List<ValidatedFile> validatedFiles = fileValidationService.validateImages(files);

        List<MediaImageResponse> uploadedImages = new ArrayList<>();
        for (ValidatedFile validatedFile : validatedFiles) {
            Media saved = mediaStorageService.save(OwnerType.PRODUCT, productId, validatedFile);

            uploadedImages.add(toMediaImageResponse(saved));
        }

        return new ProductImagesResponse(productId, uploadedImages);
    }

    public ProductImagesResponse getProductImages(String productId) {
        requireNonBlank(productId, "productId is required");
        List<Media> assets = mediaRepository.findByOwnerTypeAndOwnerIdOrderByIdAsc(OwnerType.PRODUCT, productId);

        List<MediaImageResponse> images = assets.stream()
                .map(this::toMediaImageResponse)
                .toList();

        return new ProductImagesResponse(productId, images);
    }

    public ProfileImageResponse uploadProfileImage(String userId, MultipartFile file) {
        requireNonBlank(userId, "Authenticated user is required");
        ValidatedFile validatedFile = fileValidationService.validateImage(file);

        // Keep one active avatar: remove old avatar metadata and object first.
        List<Media> existingAvatars = mediaRepository.findByOwnerTypeAndOwnerId(OwnerType.AVATAR, userId);
        mediaStorageService.delete(existingAvatars);
        Media savedAvatar = mediaStorageService.save(OwnerType.AVATAR, userId, validatedFile);

        return new ProfileImageResponse(toMediaImageResponse(savedAvatar));
    }

    public ProfileImageResponse getProfileImage(String userId) {
        requireNonBlank(userId, "userId is required");

        Media avatar = mediaRepository.findFirstByOwnerTypeAndOwnerIdOrderByIdDesc(OwnerType.AVATAR, userId)
                .orElseThrow(() -> new MediaNotFoundException("Profile image not found"));

        return new ProfileImageResponse(toMediaImageResponse(avatar));
    }

    private void requireNonBlank(String value, String message) {
        if (value == null || value.trim().isEmpty()) {
            throw new BadRequestException(message);
        }
    }

    private MediaImageResponse toMediaImageResponse(Media asset) {
        return new MediaImageResponse(
                asset.getId(),
                mediaUrlBuilder.build(asset.getObjectKey()));
    }
}
