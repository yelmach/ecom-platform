import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { MatDialog } from '@angular/material/dialog';
import { of, throwError } from 'rxjs';

import { ProductList } from './product-list';
import { ProductService } from '../../../core/services/product.service';
import { MediaService } from '../../../core/services/media.service';
import { Page, Product } from '../../../core/models/product';
import { ProductImagesResponse } from '../../../core/models/media';
import { ProductDetails } from '../product-details/product-details';

describe('ProductList Component', () => {
    let component: ProductList;
    let fixture: ComponentFixture<ProductList>;
    let productServiceSpy: jasmine.SpyObj<ProductService>;
    let mediaServiceSpy: jasmine.SpyObj<MediaService>;
    let dialogSpy: jasmine.SpyObj<MatDialog>;

    const mockProduct = {
        id: 'prod-1',
        name: 'Test Product',
        description: 'A great product',
        price: 99.99,
        sellerId: 'seller-123',
        mediaIds: ['media-1'],
    } as Product;

    const mockPage: Page<Product> = {
        content: [mockProduct],
        totalElements: 1,
        totalPages: 1,
        size: 10,
        number: 0,
        last: true
    };

    const mockImagesResponse: ProductImagesResponse = {
        productId: 'prod-1',
        images: [{ id: 'media-1', url: 'http://example.com/image.jpg' }],
    };

    beforeEach(async () => {
        productServiceSpy = jasmine.createSpyObj('ProductService', ['getAllProduct']);
        mediaServiceSpy = jasmine.createSpyObj('MediaService', [
            'getProductImages',
            'getPrimaryProductImageUrl',
        ]);
        dialogSpy = jasmine.createSpyObj('MatDialog', ['open']);

        // Setup default successful returns to prevent initialization failures
        productServiceSpy.getAllProduct.and.returnValue(of(mockPage));
        mediaServiceSpy.getProductImages.and.returnValue(of(mockImagesResponse));
        mediaServiceSpy.getPrimaryProductImageUrl.and.returnValue('http://example.com/image.jpg');

        await TestBed.configureTestingModule({
            imports: [ProductList],
            providers: [
                { provide: ProductService, useValue: productServiceSpy },
                { provide: MediaService, useValue: mediaServiceSpy },
                { provide: MatDialog, useValue: dialogSpy },
                provideNoopAnimations(),
            ],
        }).compileComponents();

        fixture = TestBed.createComponent(ProductList);
        component = fixture.componentInstance;

        // Triggers ngOnInit, which calls loadProducts()
        fixture.detectChanges();
    });

    it('should create the component and load initial products', () => {
        expect(component).toBeTruthy();
        expect(productServiceSpy.getAllProduct).toHaveBeenCalledWith(0, 10);

        // Verify state after initialization
        expect(component.products()).toEqual([mockProduct]);
        expect(component.currentPage()).toBe(1); // Incremented after load
        expect(component.isLast()).toBeTrue();
        expect(component.loading()).toBeFalse();

        // Verify image loading behavior
        expect(mediaServiceSpy.getProductImages).toHaveBeenCalledWith('prod-1');
        expect(component.productImageUrls()['prod-1']).toBe('http://example.com/image.jpg');
    });

    describe('Pagination Logic', () => {
        it('should not load more if currently loading', () => {
            productServiceSpy.getAllProduct.calls.reset();
            component.loading.set(true);
            component.loadProducts();
            expect(productServiceSpy.getAllProduct).not.toHaveBeenCalled();
        });

        it('should not load more if on the last page', () => {
            productServiceSpy.getAllProduct.calls.reset();
            component.isLast.set(true);
            component.loadProducts();
            expect(productServiceSpy.getAllProduct).not.toHaveBeenCalled();
        });
    });

    describe('UI Actions', () => {
        it('should open product details dialog', () => {
            component.openProductDetails(mockProduct);
            expect(dialogSpy.open).toHaveBeenCalledWith(ProductDetails, {
                data: mockProduct,
                width: '550px',
            });
        });
    });

    describe('Image Retrieval', () => {
        it('should return a placeholder URL if image is not loaded', () => {
            const missingProduct = { ...mockProduct, id: 'missing-1', name: 'Unknown Item' } as Product;
            expect(component.getProductImageUrl(missingProduct)).toBe('https://placehold.co/300x200/222/666?text=Unknown Item');
        });
    });
});