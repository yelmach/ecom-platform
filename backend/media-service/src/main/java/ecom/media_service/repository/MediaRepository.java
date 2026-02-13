package ecom.media_service.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

import ecom.media_service.model.Media;
import ecom.media_service.model.OwnerType;

public interface MediaRepository extends MongoRepository<Media, String> {

    List<Media> findByOwnerTypeAndOwnerIdOrderByIdAsc(OwnerType ownerType, String ownerId);

    List<Media> findByOwnerTypeAndOwnerId(OwnerType ownerType, String ownerId);

    Optional<Media> findFirstByOwnerTypeAndOwnerIdOrderByIdDesc(OwnerType ownerType, String ownerId);
}

