import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ANIMATION_MODULE_TYPE } from '@angular/platform-browser/animations';
import { Router } from '@angular/router';
import { MatDialog } from '@angular/material/dialog';
import { of, throwError } from 'rxjs';

import { Dashboard } from './dashboard';
import { ProductService } from '../../../core/services/product.service';
import { MediaService } from '../../../core/services/media.service';
import { Page, Product } from '../../../core/models/product';
import { ProductImagesResponse } from '../../../core/models/media';
import { ProductDetails } from '../../shop/product-details/product-details';

describe('Dashboard Component', () => {
    let component: Dashboard;
    let fixture: ComponentFixture<Dashboard>;
    let productServiceSpy: jasmine.SpyObj<ProductService>;
    let mediaServiceSpy: jasmine.SpyObj<MediaService>;
    let routerSpy: jasmine.SpyObj<Router>;
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
        last: true,
    };

    const mockImagesResponse: ProductImagesResponse = {
        productId: 'prod-1',
        images: [{ id: 'media-1', url: 'http://example.com/image.jpg' }],
    };

    beforeEach(async () => {
        productServiceSpy = jasmine.createSpyObj('ProductService', [
            'getMyProducts',
            'deleteProduct',
        ]);
        mediaServiceSpy = jasmine.createSpyObj('MediaService', [
            'getProductImages',
            'getPrimaryProductImageUrl',
        ]);
        routerSpy = jasmine.createSpyObj('Router', ['navigateByUrl']);
        dialogSpy = jasmine.createSpyObj('MatDialog', ['open']);

        // Setup default successful returns to prevent initialization failures
        productServiceSpy.getMyProducts.and.returnValue(of(mockPage));
        mediaServiceSpy.getProductImages.and.returnValue(of(mockImagesResponse));
        mediaServiceSpy.getPrimaryProductImageUrl.and.returnValue('http://example.com/image.jpg');

        await TestBed.configureTestingModule({
            imports: [Dashboard],
            providers: [
                { provide: ProductService, useValue: productServiceSpy },
                { provide: MediaService, useValue: mediaServiceSpy },
                { provide: Router, useValue: routerSpy },
                { provide: MatDialog, useValue: dialogSpy },
                { provide: ANIMATION_MODULE_TYPE, useValue: 'NoopAnimations' },
            ],
        }).compileComponents();

        fixture = TestBed.createComponent(Dashboard);
        component = fixture.componentInstance;

        // This triggers ngOnInit, which will call loadMore()
        fixture.detectChanges();
    });

    it('should create the component and load initial products', () => {
        expect(component).toBeTruthy();
        expect(productServiceSpy.getMyProducts).toHaveBeenCalledWith(0, 10);

        // Verify state after initialization
        expect(component.products()).toEqual([mockProduct]);
        expect(component.totalProducts()).toBe(1);
        expect(component.currentPage()).toBe(1); // Incremented after load
        expect(component.isLast()).toBeTrue();
        expect(component.loading()).toBeFalse();

        // Verify image loading
        expect(mediaServiceSpy.getProductImages).toHaveBeenCalledWith('prod-1');
        expect(component.productImageUrls()['prod-1']).toBe('http://example.com/image.jpg');
    });

    describe('Pagination Logic', () => {
        it('should not load more if currently loading', () => {
            productServiceSpy.getMyProducts.calls.reset();

            component.loading.set(true);
            component.loadMore();

            expect(productServiceSpy.getMyProducts).not.toHaveBeenCalled();
        });

        it('should not load more if on the last page', () => {
            productServiceSpy.getMyProducts.calls.reset();

            component.isLast.set(true);
            component.loadMore();

            expect(productServiceSpy.getMyProducts).not.toHaveBeenCalled();
        });

        it('should calculate canLoadMore computed signal correctly', () => {
            // Initially from setup, isLast is true
            expect(component.canLoadMore()).toBeFalse();

            component.isLast.set(false);
            component.loading.set(false);
            expect(component.canLoadMore()).toBeTrue();

            component.loading.set(true);
            expect(component.canLoadMore()).toBeFalse();
        });

        it('should reset state and reload products', () => {
            productServiceSpy.getMyProducts.calls.reset();

            // Fake changing the state
            component.currentPage.set(2);
            component.products.set([mockProduct, mockProduct]);

            component.resetAndReload();

            // Verify state was wiped before reloading
            expect(productServiceSpy.getMyProducts).toHaveBeenCalledWith(0, 10);
            expect(component.products().length).toBe(1); // Gets repopulated by the mock return
        });

        it('should handle errors during loadMore and reset loading state', () => {
            productServiceSpy.getMyProducts.and.returnValue(throwError(() => new Error('API Error')));
            component.loading.set(false);
            
            component.loadMore();
            
            expect(component.loading()).toBeFalse();
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

        it('should navigate to edit product page', () => {
            component.editProduct(mockProduct);
            expect(routerSpy.navigateByUrl).toHaveBeenCalledWith('/seller/edit/prod-1');
        });

        it('should navigate to create product page', () => {
            component.navigateToCreate();
            expect(routerSpy.navigateByUrl).toHaveBeenCalledWith('/seller/create');
        });

        it('should delete product and trigger a reload', () => {
            productServiceSpy.deleteProduct.and.returnValue(of(undefined));
            spyOn(component, 'resetAndReload'); // Spy on the component's own method

            component.deleteProduct(mockProduct);

            expect(productServiceSpy.deleteProduct).toHaveBeenCalledWith('prod-1');
            expect(component.resetAndReload).toHaveBeenCalled();
        });

        it('should not reload on failed product deletion', () => {
            productServiceSpy.deleteProduct.and.returnValue(throwError(() => new Error('Delete failed')));
            spyOn(component, 'resetAndReload');
            // Suppress the expected console error from the test output
            spyOn(console, 'error');

            component.deleteProduct(mockProduct);

            expect(productServiceSpy.deleteProduct).toHaveBeenCalledWith('prod-1');
            expect(component.resetAndReload).not.toHaveBeenCalled();
        });
    });

    describe('Image Retrieval', () => {
        it('should return the specific product image URL if loaded', () => {
            // Our setup already loaded the image for prod-1
            const url = component.getProductImageUrl(mockProduct);
            expect(url).toBe('http://example.com/image.jpg');
        });

        it('should return a placeholder URL if image is not loaded', () => {
            const missingProduct = { ...mockProduct, id: 'missing-1', name: 'Unknown Product' } as Product;
            const url = component.getProductImageUrl(missingProduct);
            expect(url).toBe('https://placehold.co/300x200/222/666?text=Unknown Product');
        });
    });
});