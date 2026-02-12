import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Product } from '../models/product';
import { Observable } from 'rxjs';

@Injectable({
  providedIn: 'root',
})
export class ProductService {
  private http = inject(HttpClient);

  getAllProduct(): Observable<Product[]> {
    return this.http.get<Product[]>('/products');
  }

  getSingleProduct(postId: String) : Observable<Product> {
    return this.http.get<Product>(`/products/${postId}`)
  }
}
