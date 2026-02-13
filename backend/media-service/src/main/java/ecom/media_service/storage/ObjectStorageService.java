package ecom.media_service.storage;

public interface ObjectStorageService {

    void upload(String objectKey, byte[] bytes, String contentType);

    void delete(String objectKey);
}

