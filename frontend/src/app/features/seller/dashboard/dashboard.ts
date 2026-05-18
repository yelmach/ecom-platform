import { CurrencyPipe } from '@angular/common';
import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatDialog } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';
import { MatMenuModule } from '@angular/material/menu';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { Router } from '@angular/router';
import { finalize } from 'rxjs';

import { Page, Product } from '../../../core/models/product';
import { MediaService } from '../../../core/services/media.service';
import { ProductService } from '../../../core/services/product.service';
import { InfiniteScrollDirective } from '../../../shared/directives/infinite-scroll.directive';
import { ProductDetails } from '../../shop/product-details/product-details';

@Component({
  selector: 'app-dashboard',
  imports: [
    CurrencyPipe,
    MatButtonModule,
    MatIconModule,
    MatMenuModule,
    MatProgressSpinnerModule,
    InfiniteScrollDirective,
  ],
  templateUrl: './dashboard.html',
  styleUrl: './dashboard.scss',
})
export class Dashboard implements OnInit {
  private readonly productService = inject(ProductService);
  private readonly mediaService = inject(MediaService);
  private readonly router = inject(Router);
  private readonly dialog = inject(MatDialog);
  private readonly pageSize = 10;

  readonly products = signal<Product[]>([]);
  readonly totalProducts = signal(0);
  readonly currentPage = signal(0);
  readonly isLast = signal(false);
  readonly loading = signal(false);
  readonly productImageUrls = signal<Record<string, string>>({});
  readonly canLoadMore = computed(() => !this.loading() && !this.isLast());

  ngOnInit(): void {
    this.loadMore();
  }

  loadMore(): void {
    if (this.loading() || this.isLast()) {
      return;
    }

    this.loading.set(true);
    this.productService
      .getMyProducts(this.currentPage(), this.pageSize)
      .pipe(finalize(() => this.loading.set(false)))
      .subscribe({
        next: (page) => this.appendPage(page),
        error: () => {},
      });
  }

  resetAndReload(): void {
    this.products.set([]);
    this.productImageUrls.set({});
    this.currentPage.set(0);
    this.isLast.set(false);
    this.loadMore();
  }

  openProductDetails(product: Product): void {
    this.dialog.open(ProductDetails, { data: product, width: '550px' });
  }

  editProduct(product: Product): void {
    this.router.navigateByUrl(`/seller/edit/${product.id}`);
  }

  navigateToCreate(): void {
    this.router.navigateByUrl('/seller/create');
  }

  deleteProduct(product: Product): void {
    this.productService.deleteProduct(product.id).subscribe({
      next: () => this.resetAndReload(),
      error: (error) => console.error(error),
    });
  }

  getProductImageUrl(product: Product): string {
    return this.productImageUrls()[product.id] ?? `https://placehold.co/300x200/222/666?text=${product.name}`;
  }

  private appendPage(page: Page<Product>): void {
    this.products.update((products) => [...products, ...page.content]);
    this.totalProducts.set(page.totalElements);
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
