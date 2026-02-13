import { Component, inject, OnInit, signal } from '@angular/core';
import { Router } from '@angular/router';
import { CurrencyPipe } from '@angular/common';
import { MatButton, MatIconButton } from '@angular/material/button';
import { MatIcon } from '@angular/material/icon';
import { MatMenuModule } from '@angular/material/menu';
import { MatDialog } from '@angular/material/dialog';
import { catchError, forkJoin, map, of } from 'rxjs';
import { ProductService } from '../../../core/services/product.service';
import { Product } from '../../../core/models/product';
import { ProductDetails } from '../../shop/product-details/product-details';
import { MediaService } from '../../../core/services/media.service';

@Component({
  selector: 'app-dashboard',
  imports: [CurrencyPipe, MatButton, MatIconButton, MatIcon, MatMenuModule],
  templateUrl: './dashboard.html',
  styleUrl: './dashboard.scss',
})
export class Dashboard implements OnInit {
  private productService = inject(ProductService);
  private mediaService = inject(MediaService);
  private router = inject(Router);
  private dialog = inject(MatDialog);

  products = signal<Product[]>([]);
  productImageUrls = signal<Record<string, string>>({});
  totalProducts = signal(0);

  ngOnInit() {
    this.loadProducts();
  }

  loadProducts() {
    this.productService.getMyProducts().subscribe((page) => {
      this.products.set(page.content);
      this.totalProducts.set(page.totalElements);
      this.loadProductImageUrls(page.content);
    });
  }

  openProductDetails(product: Product): void {
    this.dialog.open(ProductDetails, {
      data: product,
      width: '550px',
    });
  }

  editProduct(product: Product): void {
    this.router.navigateByUrl(`/seller/edit/${product.id}`);
  }

  deleteProduct(product: Product): void {
    this.productService.deleteProduct(product.id).subscribe(() => {
      this.loadProducts();
    });
  }

  navigateToCreate() {
    this.router.navigateByUrl('/seller/create');
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
