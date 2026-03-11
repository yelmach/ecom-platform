package ecom.media_service.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ecom.media_service.config.MediaProperties;

class MediaUrlBuilderTest {

    private MediaUrlBuilder mediaUrlBuilder;

    @BeforeEach
    void setUp() {
        MediaProperties mediaProperties = new MediaProperties();
        mediaProperties.setPublicBaseUrl("http://cdn.local/");
        mediaUrlBuilder = new MediaUrlBuilder(mediaProperties);
    }

    @Test
    void build_ShouldConcatenateBaseAndObjectKey() {
        assertEquals("http://cdn.local/path/to/file.png", mediaUrlBuilder.build("path/to/file.png"));
    }

    @Test
    void build_ShouldNormalizeLeadingSlashInObjectKey() {
        assertEquals("http://cdn.local/path/to/file.png", mediaUrlBuilder.build("/path/to/file.png"));
    }

    @Test
    void build_ShouldHandleNullValues() {
        MediaProperties mediaProperties = new MediaProperties();
        mediaProperties.setPublicBaseUrl(null);
        MediaUrlBuilder builder = new MediaUrlBuilder(mediaProperties);

        assertEquals("/", builder.build(null));
    }
}
