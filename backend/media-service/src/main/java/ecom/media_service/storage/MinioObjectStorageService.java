package ecom.media_service.storage;

import java.io.ByteArrayInputStream;

import org.springframework.stereotype.Service;

import ecom.media_service.config.MediaProperties;
import ecom.media_service.exception.StorageException;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MinioObjectStorageService implements ObjectStorageService {

    private final MinioClient minioClient;
    private final MediaProperties mediaProperties;

    @Override
    public void upload(String objectKey, byte[] bytes, String contentType) {
        try (ByteArrayInputStream inputStream = new ByteArrayInputStream(bytes)) {
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(mediaProperties.getMinio().getBucket())
                            .object(objectKey)
                            .stream(inputStream, bytes.length, -1)
                            .contentType(contentType)
                            .build());
        } catch (Exception ex) {
            throw new StorageException("Failed to upload object to MinIO", ex);
        }
    }

    @Override
    public void delete(String objectKey) {
        try {
            minioClient.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(mediaProperties.getMinio().getBucket())
                            .object(objectKey)
                            .build());
        } catch (Exception ex) {
            throw new StorageException("Failed to delete object from MinIO", ex);
        }
    }
}
