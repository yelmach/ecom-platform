import { Component, inject, OnInit, signal } from '@angular/core';
import { Product } from '../../../core/models/product';
import { ProductService } from '../../../core/services/product.service';
import { CurrencyPipe } from '@angular/common';
import { MatButton } from '@angular/material/button';
import { MatIcon } from '@angular/material/icon';
import { MatDialog } from '@angular/material/dialog';
import { catchError, forkJoin, map, of } from 'rxjs';
import { ProductDetails } from '../product-details/product-details';
import { MediaService } from '../../../core/services/media.service';

@Component({
  selector: 'app-product-list',
  imports: [CurrencyPipe, MatButton, MatIcon],
  templateUrl: './product-list.html',
  styleUrl: './product-list.scss',
})
export class ProductList {
  private productService = inject(ProductService);
  private mediaService = inject(MediaService);
  private dialog = inject(MatDialog);

  products = signal<Product[]>([]);
  productImageUrls = signal<Record<string, string>>({});

  ngOnInit() {
    this.productService.getAllProduct().subscribe((page) => {
      this.products.set(page.content);
      this.loadProductImageUrls(page.content);
    });
  }

  openProductDetails(product: Product): void {
    this.dialog.open(ProductDetails, {
      data: product,
      width: '550px',
    });
  }

  getProductImageUrl(product: Product): string {
    return this.productImageUrls()[product.id] ?? `https://placehold.co/300x200/222/666?text=${product.name}`;
  }

  private loadProductImageUrls(products: Product[]): void {
    if (!products.length) {
      this.productImageUrls.set({});
      return;
    }

    const requests = products.map((product) =>
      this.mediaService.getProductImages(product.id).pipe(
        map((response) => ({
          productId: product.id,
          url: this.mediaService.getPrimaryProductImageUrl(response, product.mediaIds) ?? '',
        })),
        catchError(() => of({ productId: product.id, url: '' })),
      ));

    forkJoin(requests).subscribe((results) => {
      const imageMap: Record<string, string> = {};
      results.forEach((result) => {
        if (result.url) {
          imageMap[result.productId] = result.url;
        }
      });
      this.productImageUrls.set(imageMap);
    });
  }
}
