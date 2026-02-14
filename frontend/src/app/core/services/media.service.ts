import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { MediaImage, ProductImagesResponse, ProfileImageResponse } from '../models/media';

@Injectable({
  providedIn: 'root',
})
export class MediaService {
  private http = inject(HttpClient);

  uploadProfile(file: File): Observable<ProfileImageResponse> {
    const formData = new FormData();
    formData.append('file', file);

    return this.http.post<ProfileImageResponse>('/media/profile', formData);
  }

  getProfile(userId: string): Observable<ProfileImageResponse> {
    return this.http.get<ProfileImageResponse>(`/media/profile/${userId}`);
  }

  uploadProductImages(productId: string, files: File[]): Observable<ProductImagesResponse> {
    const formData = new FormData();
    formData.append('productId', productId);
    files.forEach((file) => formData.append('files', file));

    return this.http.post<ProductImagesResponse>('/media/images', formData);
  }

  getProductImages(productId: string): Observable<ProductImagesResponse> {
    return this.http.get<ProductImagesResponse>(`/media/images/${productId}`);
  }

  getOrderedProductImages(response: ProductImagesResponse, mediaIds?: string[] | null): MediaImage[] {
    const hasCanonicalMediaIds = Array.isArray(mediaIds);
    const orderedIds = hasCanonicalMediaIds
      ? mediaIds
      : response.images.map((image) => image.id);

    const imageById = new Map(response.images.map((image) => [image.id, image]));

    return orderedIds
      .map((mediaId) => imageById.get(mediaId))
      .filter((image): image is MediaImage => !!image);
  }

  getPrimaryProductImageUrl(response: ProductImagesResponse, mediaIds?: string[] | null): string | null {
    return this.getOrderedProductImages(response, mediaIds)[0]?.url ?? null;
  }
}
