import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { MediaService } from './media.service';
import { MediaImage, ProductImagesResponse, ProfileImageResponse } from '../models/media';

describe('MediaService', () => {
  let service: MediaService;
  let httpTestingController: HttpTestingController;

  // Mock Data
  const mockProfileResponse: ProfileImageResponse = {
    avatar: {
      id: 'media-1',
      url: 'http://example.com/avatar.jpg'
    }
  };

  const mockMediaImages: MediaImage[] = [
    { id: 'img-1', url: 'http://example.com/1.jpg' },
    { id: 'img-2', url: 'http://example.com/2.jpg' },
    { id: 'img-3', url: 'http://example.com/3.jpg' }
  ];

  const mockProductImagesResponse: ProductImagesResponse = {
    productId: 'prod-123',
    images: mockMediaImages
  };

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [MediaService]
    });

    service = TestBed.inject(MediaService);
    httpTestingController = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    // Ensure there are no outstanding requests after each test
    httpTestingController.verify();
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  describe('#uploadProfile', () => {
    it('should send a POST request with the file in FormData', () => {
      const mockFile = new File([''], 'avatar.jpg', { type: 'image/jpeg' });

      service.uploadProfile(mockFile).subscribe(response => {
        expect(response).toEqual(mockProfileResponse);
      });

      const req = httpTestingController.expectOne('/media/profile');
      expect(req.request.method).toBe('POST');
      
      // Validate the FormData contents
      const formData = req.request.body as FormData;
      expect(formData.get('file')).toBe(mockFile);

      req.flush(mockProfileResponse);
    });
  });

  describe('#getProfile', () => {
    it('should send a GET request to retrieve the profile image', () => {
      const userId = 'user-123';
      
      service.getProfile(userId).subscribe(response => {
        expect(response).toEqual(mockProfileResponse);
      });

      const req = httpTestingController.expectOne(`/media/profile/${userId}`);
      expect(req.request.method).toBe('GET');
      req.flush(mockProfileResponse);
    });
  });

  describe('#uploadProductImages', () => {
    it('should send a POST request with productId and multiple files in FormData', () => {
      const productId = 'prod-123';
      const mockFiles = [
        new File([''], 'prod1.jpg', { type: 'image/jpeg' }),
        new File([''], 'prod2.jpg', { type: 'image/jpeg' })
      ];

      service.uploadProductImages(productId, mockFiles).subscribe(response => {
        expect(response).toEqual(mockProductImagesResponse);
      });

      const req = httpTestingController.expectOne('/media/images');
      expect(req.request.method).toBe('POST');
      
      // Validate the FormData contents
      const formData = req.request.body as FormData;
      expect(formData.get('productId')).toBe(productId);
      expect(formData.getAll('files').length).toBe(2);
      expect(formData.getAll('files')).toEqual(mockFiles);

      req.flush(mockProductImagesResponse);
    });
  });

  describe('#getProductImages', () => {
    it('should send a GET request to retrieve product images', () => {
      const productId = 'prod-123';
      
      service.getProductImages(productId).subscribe(response => {
        expect(response).toEqual(mockProductImagesResponse);
      });

      const req = httpTestingController.expectOne(`/media/images/${productId}`);
      expect(req.request.method).toBe('GET');
      req.flush(mockProductImagesResponse);
    });
  });

  describe('#getOrderedProductImages', () => {
    it('should return images in the order of provided mediaIds', () => {
      const orderedIds = ['img-3', 'img-1'];
      const result = service.getOrderedProductImages(mockProductImagesResponse, orderedIds);
      
      expect(result.length).toBe(2);
      expect(result[0].id).toBe('img-3');
      expect(result[1].id).toBe('img-1');
    });

    it('should return default image order if mediaIds are not provided', () => {
      const result = service.getOrderedProductImages(mockProductImagesResponse);
      
      expect(result.length).toBe(3);
      expect(result).toEqual(mockMediaImages);
    });

    it('should filter out mediaIds that do not exist in the response', () => {
      const orderedIds = ['img-2', 'non-existent-id'];
      const result = service.getOrderedProductImages(mockProductImagesResponse, orderedIds);
      
      expect(result.length).toBe(1);
      expect(result[0].id).toBe('img-2');
    });
  });

  describe('#getPrimaryProductImageUrl', () => {
    it('should return the URL of the first image in the ordered array', () => {
      const orderedIds = ['img-2', 'img-1'];
      const result = service.getPrimaryProductImageUrl(mockProductImagesResponse, orderedIds);
      
      expect(result).toBe('http://example.com/2.jpg'); // Maps back to img-2
    });

    it('should return null if there are no images available', () => {
      const emptyResponse: ProductImagesResponse = { productId: 'prod-123', images: [] };
      const result = service.getPrimaryProductImageUrl(emptyResponse);
      
      expect(result).toBeNull();
    });
  });
});