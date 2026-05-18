import { CurrencyPipe } from '@angular/common';
import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatDialog } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { finalize } from 'rxjs';

import { Page, Product } from '../../../core/models/product';
import { MediaService } from '../../../core/services/media.service';
import { ProductService } from '../../../core/services/product.service';
import { InfiniteScrollDirective } from '../../../shared/directives/infinite-scroll.directive';
import { ProductDetails } from '../product-details/product-details';

@Component({
  selector: 'app-product-list',
  imports: [
    CurrencyPipe,
    MatButtonModule,
    MatIconModule,
    MatProgressSpinnerModule,
    InfiniteScrollDirective,
  ],
  templateUrl: './product-list.html',
  styleUrl: './product-list.scss',
})
export class ProductList implements OnInit {
  private readonly productService = inject(ProductService);
  private readonly mediaService = inject(MediaService);
  private readonly dialog = inject(MatDialog);
  private readonly pageSize = 10;

  readonly products = signal<Product[]>([]);
  readonly currentPage = signal(0);
  readonly isLast = signal(false);
  readonly loading = signal(false);
  readonly productImageUrls = signal<Record<string, string>>({});
  readonly canLoadMore = computed(() => !this.loading() && !this.isLast());

  ngOnInit(): void {
    this.loadProducts();
  }

  loadProducts(): void {
    if (this.loading() || this.isLast()) {
      return;
    }

    this.loading.set(true);
    this.productService
      .getAllProduct(this.currentPage(), this.pageSize)
      .pipe(finalize(() => this.loading.set(false)))
      .subscribe({
        next: (page) => this.appendPage(page),
        error: () => {},
      });
  }

  openProductDetails(product: Product): void {
    this.dialog.open(ProductDetails, { data: product, width: '550px' });
  }

  getProductImageUrl(product: Product): string {
    return this.productImageUrls()[product.id] ?? `https://placehold.co/300x200/222/666?text=${product.name}`;
  }

  private appendPage(page: Page<Product>): void {
    this.products.update((products) => [...products, ...page.content]);
    this.currentPage.update((pageNumber) => pageNumber + 1);
    this.isLast.set(page.last);
    page.content.forEach((product) => this.loadProductImage(product));
  }

  private loadProductImage(product: Product): void {
    this.mediaService.getProductImages(product.id).subscribe({
      next: (response) => {
        const imageUrl = this.mediaService.getPrimaryProductImageUrl(response, product.mediaIds);
        if (imageUrl) {
          this.productImageUrls.update((urls) => ({ ...urls, [product.id]: imageUrl }));
        }
      },
      error: () => {},
    });
  }
}
