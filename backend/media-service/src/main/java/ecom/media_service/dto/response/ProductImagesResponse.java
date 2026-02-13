package ecom.media_service.dto.response;

import java.util.List;

public record ProductImagesResponse(
        String productId,
        List<MediaImageResponse> images) {
}

