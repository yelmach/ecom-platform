import { HttpClient, HttpParams } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Page, Product, ProductFormData } from '../models/product';
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

  getSingleProduct(productId: String): Observable<Product> {
    return this.http.get<Product>(`/products/${productId}`);
  }

  createProduct(productData: ProductFormData): Observable<Product> {
    return this.http.post<Product>('/products', productData);
  }
}
