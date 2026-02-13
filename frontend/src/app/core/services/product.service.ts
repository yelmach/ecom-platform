import { HttpClient, HttpParams } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Page, Product, ProductFormData, ProductUpdateData } from '../models/product';
import { Observable } from 'rxjs';

@Injectable({
  providedIn: 'root',
})
export class ProductService {
  private http = inject(HttpClient);

  getAllProduct(page = 0, size = 10): Observable<Page<Product>> {
    const params = new HttpParams().set('page', page).set('size', size);
    return this.http.get<Page<Product>>('/products', { params });
  }

  getMyProducts(page = 0, size = 10): Observable<Page<Product>> {
    const params = new HttpParams().set('page', page).set('size', size);
    return this.http.get<Page<Product>>('/products/me', { params });
  }

  getSingleProduct(productId: string): Observable<Product> {
    return this.http.get<Product>(`/products/${productId}`);
  }

  createProduct(productData: ProductFormData): Observable<Product> {
    return this.http.post<Product>('/products', productData);
  }

  updateProduct(productId: string, productData: ProductUpdateData): Observable<Product> {
    return this.http.put<Product>(`/products/${productId}`, productData);
  }

  deleteProduct(productId: string): Observable<void> {
    return this.http.delete<void>(`/products/${productId}`);
  }
}
