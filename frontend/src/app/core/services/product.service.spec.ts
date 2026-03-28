import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { ProductService } from './product.service';
import { Page, Product, ProductFormData, ProductUpdateData } from '../models/product';

describe('ProductService', () => {
    let service: ProductService;
    let httpTestingController: HttpTestingController;

    // Mock data to be used across tests
    const mockProduct = {
        id: '1',
        name: 'Test Product',
        description: 'A great product',
        price: 99.99,
    } as Product;

    const mockPage = {
        content: [mockProduct],
        totalElements: 1,
        totalPages: 1,
        size: 10,
        number: 0,
    } as Page<Product>;

    beforeEach(() => {
        TestBed.configureTestingModule({
            imports: [HttpClientTestingModule],
            providers: [ProductService],
        });

        service = TestBed.inject(ProductService);
        httpTestingController = TestBed.inject(HttpTestingController);
    });

    afterEach(() => {
        // Ensure that there are no outstanding requests after each test
        httpTestingController.verify();
    });

    it('should be created', () => {
        expect(service).toBeTruthy();
    });

    describe('#getAllProduct', () => {
        it('should send a GET request with default pagination params', () => {
            service.getAllProduct().subscribe((page) => {
                expect(page).toEqual(mockPage);
            });

            const req = httpTestingController.expectOne('/products?page=0&size=10');
            expect(req.request.method).toBe('GET');
            req.flush(mockPage);
        });

        it('should send a GET request with custom pagination params', () => {
            service.getAllProduct(2, 20).subscribe((page) => {
                expect(page).toEqual(mockPage);
            });

            const req = httpTestingController.expectOne('/products?page=2&size=20');
            expect(req.request.method).toBe('GET');
            req.flush(mockPage);
        });
    });

    describe('#getMyProducts', () => {
        it('should send a GET request to /me endpoint with pagination', () => {
            service.getMyProducts(1, 15).subscribe((page) => {
                expect(page).toEqual(mockPage);
            });

            const req = httpTestingController.expectOne('/products/me?page=1&size=15');
            expect(req.request.method).toBe('GET');
            req.flush(mockPage);
        });
    });

    describe('#getSingleProduct', () => {
        it('should send a GET request for a specific product ID', () => {
            const productId = '123';
            service.getSingleProduct(productId).subscribe((product) => {
                expect(product).toEqual(mockProduct);
            });

            const req = httpTestingController.expectOne(`/products/${productId}`);
            expect(req.request.method).toBe('GET');
            req.flush(mockProduct);
        });
    });

    describe('#createProduct', () => {
        it('should send a POST request with product data', () => {
            const formData = { name: 'New Product', price: 50 } as ProductFormData;

            service.createProduct(formData).subscribe((product) => {
                expect(product).toEqual(mockProduct);
            });

            const req = httpTestingController.expectOne('/products');
            expect(req.request.method).toBe('POST');
            expect(req.request.body).toEqual(formData);
            req.flush(mockProduct);
        });
    });

    describe('#updateProduct', () => {
        it('should send a PUT request with product ID and update data', () => {
            const updateData = { name: 'Updated Product' } as ProductUpdateData;

            service.updateProduct('123', updateData).subscribe((product) => {
                expect(product).toEqual(mockProduct);
            });

            const req = httpTestingController.expectOne('/products/123');
            expect(req.request.method).toBe('PUT');
            expect(req.request.body).toEqual(updateData);
            req.flush(mockProduct);
        });
    });

    describe('#deleteProduct', () => {
        it('should send a DELETE request for a specific product ID', () => {
            service.deleteProduct('123').subscribe();

            const req = httpTestingController.expectOne('/products/123');
            expect(req.request.method).toBe('DELETE');
            req.flush(null);
        });
    });
});