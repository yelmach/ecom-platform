package ecom.media_service.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import ecom.media_service.service.MediaService;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/internal/media/avatars")
@RequiredArgsConstructor
public class InternalMediaController {
    private final MediaService mediaService;

    @GetMapping("/{mediaId}/ownership")
    public ResponseEntity<Void> verifyOwnership(@PathVariable String mediaId,
            @RequestHeader("X-User-Id") String userId) {
        mediaService.verifyAvatarOwnership(mediaId, userId);
        return ResponseEntity.noContent().build();
    }

}
