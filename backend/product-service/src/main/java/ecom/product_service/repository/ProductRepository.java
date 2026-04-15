package ecom.product_service.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import ecom.product_service.model.Product;

@Repository
public interface ProductRepository extends MongoRepository<Product, String> {
    Page<Product> findBySellerId(String sellerId, Pageable pageable);

    @Query("""
            {
              'category': { '$regex': ?0, '$options': 'i' },
              '$or': [
                { 'name': { '$regex': ?1, '$options': 'i' } },
                { 'description': { '$regex': ?1, '$options': 'i' } }
              ],
              'price': { '$gte': ?2, '$lte': ?3 }
            }
            """)
    Page<Product> searchByCategoryAndKeyword(String category, String keyword, double minPrice, double maxPrice,
            Pageable pageable);

    @Query("""
            {
              '$or': [
                { 'name': { '$regex': ?0, '$options': 'i' } },
                { 'description': { '$regex': ?0, '$options': 'i' } }
              ],
              'price': { '$gte': ?1, '$lte': ?2 }
            }
            """)
    Page<Product> searchByKeyword(String keyword, double minPrice, double maxPrice, Pageable pageable);

    @Query("""
            {
              'category': { '$regex': ?0, '$options': 'i' },
              'price': { '$gte': ?1, '$lte': ?2 }
            }
            """)
    Page<Product> searchByCategory(String category, double minPrice, double maxPrice, Pageable pageable);

    @Query("""
            {
              'price': { '$gte': ?0, '$lte': ?1 }
            }
            """)
    Page<Product> searchByPriceRange(double minPrice, double maxPrice, Pageable pageable);
}
