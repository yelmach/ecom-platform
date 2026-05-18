import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { Page, Product, ProductFormData, ProductUpdateData } from '../models/product';

@Injectable({
  providedIn: 'root',
})
export class ProductService {
  private readonly http = inject(HttpClient);

  getAllProduct(page = 0, size = 10): Observable<Page<Product>> {
    return this.http.get<Page<Product>>(`/products?page=${page}&size=${size}`);
  }

  getMyProducts(page = 0, size = 10): Observable<Page<Product>> {
    return this.http.get<Page<Product>>(`/products/me?page=${page}&size=${size}`);
  }

  getSingleProduct(productId: string): Observable<Product> {
    return this.http.get<Product>(`/products/${productId}`);
  }

  createProduct(payload: ProductFormData): Observable<Product> {
    return this.http.post<Product>('/products', payload);
  }

  updateProduct(productId: string, payload: ProductUpdateData): Observable<Product> {
    return this.http.put<Product>(`/products/${productId}`, payload);
  }

  deleteProduct(productId: string): Observable<void> {
    return this.http.delete<void>(`/products/${productId}`);
  }
}
