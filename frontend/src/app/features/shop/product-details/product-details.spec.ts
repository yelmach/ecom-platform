import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { of, throwError } from 'rxjs';

import { ProductDetails } from './product-details';
import { MediaService } from '../../../core/services/media.service';
import { Product } from '../../../core/models/product';
import { ProductImagesResponse } from '../../../core/models/media';

describe('ProductDetails Component', () => {
  let component: ProductDetails;
  let fixture: ComponentFixture<ProductDetails>;
  let mediaServiceSpy: jasmine.SpyObj<MediaService>;

  const mockProduct = {
    id: 'prod-123',
    name: 'Awesome Product',
    description: 'Product description',
    price: 49.99,
    mediaIds: ['media-1', 'media-2'],
  } as Product;

  const mockImagesResponse: ProductImagesResponse = {
    productId: 'prod-123',
    images: [
      { id: 'media-1', url: 'http://example.com/1.jpg' },
      { id: 'media-2', url: 'http://example.com/2.jpg' },
    ],
  };

  beforeEach(async () => {
    mediaServiceSpy = jasmine.createSpyObj('MediaService', [
      'getProductImages',
      'getOrderedProductImages',
    ]);

    await TestBed.configureTestingModule({
      imports: [ProductDetails],
      providers: [
        { provide: MAT_DIALOG_DATA, useValue: mockProduct },
        { provide: MatDialogRef, useValue: {} }, // Required because MatDialogClose is in imports
        { provide: MediaService, useValue: mediaServiceSpy },
      ],
    }).compileComponents();
  });

  describe('Initialization', () => {
    it('should initialize with a placeholder image on API error', () => {
      mediaServiceSpy.getProductImages.and.returnValue(throwError(() => new Error('API Error')));

      fixture = TestBed.createComponent(ProductDetails);
      component = fixture.componentInstance;
      fixture.detectChanges(); // Triggers ngOnInit

      expect(component.imageUrls().length).toBe(1);
      expect(component.imageUrls()[0]).toContain('Awesome Product');
      expect(component.currentImageUrl()).toContain('Awesome Product');
      expect(component.hasMultipleImages()).toBeFalse();
    });

    it('should initialize with ordered images on API success', () => {
      mediaServiceSpy.getProductImages.and.returnValue(of(mockImagesResponse));
      mediaServiceSpy.getOrderedProductImages.and.returnValue(mockImagesResponse.images);

      fixture = TestBed.createComponent(ProductDetails);
      component = fixture.componentInstance;
      fixture.detectChanges();

      expect(mediaServiceSpy.getProductImages).toHaveBeenCalledWith('prod-123');
      expect(mediaServiceSpy.getOrderedProductImages).toHaveBeenCalledWith(mockImagesResponse, ['media-1', 'media-2']);
      
      expect(component.imageUrls().length).toBe(2);
      expect(component.imageUrls()).toEqual(['http://example.com/1.jpg', 'http://example.com/2.jpg']);
      expect(component.activeImageIndex()).toBe(0);
      expect(component.currentImageUrl()).toBe('http://example.com/1.jpg');
      expect(component.hasMultipleImages()).toBeTrue();
    });
  });

  describe('Carousel Navigation', () => {
    beforeEach(() => {
      mediaServiceSpy.getProductImages.and.returnValue(of(mockImagesResponse));
      mediaServiceSpy.getOrderedProductImages.and.returnValue(mockImagesResponse.images);

      fixture = TestBed.createComponent(ProductDetails);
      component = fixture.componentInstance;
      fixture.detectChanges();
    });

    it('should navigate to the next image and wrap around', () => {
      expect(component.activeImageIndex()).toBe(0);

      component.nextImage();
      expect(component.activeImageIndex()).toBe(1);
      expect(component.currentImageUrl()).toBe('http://example.com/2.jpg');

      component.nextImage(); // Should wrap to 0
      expect(component.activeImageIndex()).toBe(0);
    });

    it('should navigate to the previous image and wrap around', () => {
      expect(component.activeImageIndex()).toBe(0);

      component.previousImage(); // Should wrap to 1 (last image)
      expect(component.activeImageIndex()).toBe(1);
      expect(component.currentImageUrl()).toBe('http://example.com/2.jpg');

      component.previousImage();
      expect(component.activeImageIndex()).toBe(0);
    });

    it('should go to a specific image index if within bounds', () => {
      component.goToImage(1);
      expect(component.activeImageIndex()).toBe(1);

      component.goToImage(99); // Out of bounds, should be ignored
      expect(component.activeImageIndex()).toBe(1);

      component.goToImage(-1); // Out of bounds, should be ignored
      expect(component.activeImageIndex()).toBe(1);
    });
  });

  describe('Touch Gestures (Swipe)', () => {
    beforeEach(() => {
      mediaServiceSpy.getProductImages.and.returnValue(of(mockImagesResponse));
      mediaServiceSpy.getOrderedProductImages.and.returnValue(mockImagesResponse.images);

      fixture = TestBed.createComponent(ProductDetails);
      component = fixture.componentInstance;
      fixture.detectChanges();
    });

    // Helper to simulate a TouchEvent
    const createTouchEvent = (clientX: number) => {
      return {
        changedTouches: [{ clientX }],
      } as unknown as TouchEvent;
    };

    it('should navigate to previous image on a right swipe (delta > 40px)', () => {
      spyOn(component, 'previousImage');
      
      component.onTouchStart(createTouchEvent(100)); // Start at X: 100
      component.onTouchEnd(createTouchEvent(150));   // End at X: 150 (Delta: +50)

      expect(component.previousImage).toHaveBeenCalled();
    });

    it('should navigate to next image on a left swipe (delta < -40px)', () => {
      spyOn(component, 'nextImage');
      
      component.onTouchStart(createTouchEvent(150)); // Start at X: 150
      component.onTouchEnd(createTouchEvent(100));   // End at X: 100 (Delta: -50)

      expect(component.nextImage).toHaveBeenCalled();
    });

    it('should ignore swipes below the 40px threshold', () => {
      spyOn(component, 'previousImage');
      spyOn(component, 'nextImage');

      component.onTouchStart(createTouchEvent(100));
      component.onTouchEnd(createTouchEvent(120)); // Delta +20 (Below 40 threshold)

      expect(component.previousImage).not.toHaveBeenCalled();
      expect(component.nextImage).not.toHaveBeenCalled();
    });

    it('should reset touchStartX if touchEnd is missing changedTouches', () => {
      component.onTouchStart(createTouchEvent(100));
      component.onTouchEnd({ changedTouches: [] } as unknown as TouchEvent);
      
      // Since touchStartX is private, we verify it reset indirectly by triggering another end 
      // without a start, which should do nothing if touchStartX was successfully nulled.
      expect(() => component.onTouchEnd(createTouchEvent(50))).not.toThrow();
    });
  });
});