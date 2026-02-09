package ecom.user_service.repository;

import java.util.Optional;

import ecom.user_service.models.User;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface UserRepository extends MongoRepository<User, String> {
    Optional<User> findByEmail();

    boolean existByEmail();
}
