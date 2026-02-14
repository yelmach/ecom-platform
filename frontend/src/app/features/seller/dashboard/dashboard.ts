import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { Router } from '@angular/router';
import { CurrencyPipe } from '@angular/common';
import { MatButton, MatIconButton } from '@angular/material/button';
import { MatIcon } from '@angular/material/icon';
import { MatMenuModule } from '@angular/material/menu';
import { MatDialog } from '@angular/material/dialog';
import { catchError, forkJoin, map, of } from 'rxjs';
import { MediaService } from '../../../core/services/media.service';
import { MatProgressSpinner } from '@angular/material/progress-spinner';
import { ProductService } from '../../../core/services/product.service';
import { Product } from '../../../core/models/product';
import { ProductDetails } from '../../shop/product-details/product-details';
import { InfiniteScrollDirective } from '../../../shared/directives/infinite-scroll.directive';

@Component({
  selector: 'app-dashboard',
  imports: [
    CurrencyPipe,
    MatButton,
    MatIconButton,
    MatIcon,
    MatMenuModule,
    MatProgressSpinner,
    InfiniteScrollDirective,
  ],
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
  currentPage = signal(0);
  isLast = signal(false);
  loading = signal(false);

  canLoadMore = computed(() => !this.loading() && !this.isLast());

  ngOnInit(): void {
    this.loadMore();
  }

  loadMore(): void {
    if (this.loading() || this.isLast()) return;

    this.loading.set(true);
    this.productService.getMyProducts(this.currentPage(), 10).subscribe({
      next: (page) => {
        this.products.update((current) => [...current, ...page.content]);
        this.totalProducts.set(page.totalElements);
        this.loadProductImageUrls(page.content);
        this.isLast.set(page.last);
        this.currentPage.update((p) => p + 1);
        this.loading.set(false);
      },
      error: () => {
        this.loading.set(false);
      },
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
      this.resetAndReload();
    });
  }

  navigateToCreate(): void {
    this.router.navigateByUrl('/seller/create');
  }

  getProductImageUrl(product: Product): string {
    return (
      this.productImageUrls()[product.id] ??
      `https://placehold.co/300x200/222/666?text=${product.name}`
    );
  }

  private loadProductImageUrls(products: Product[]): void {
    if (!products.length) {
      return;
    }

    const requests = products.map((product) =>
      this.mediaService.getProductImages(product.id).pipe(
        map((response) => ({
          productId: product.id,
          url: this.mediaService.getPrimaryProductImageUrl(response, product.mediaIds) ?? '',
        })),
        catchError(() => of({ productId: product.id, url: '' })),
      ),
    );

    forkJoin(requests).subscribe((results) => {
      this.productImageUrls.update((current) => {
        const imageMap = { ...current };
        results.forEach((result) => {
          if (result.url) {
            imageMap[result.productId] = result.url;
          } else {
            delete imageMap[result.productId];
          }
        });
        return imageMap;
      });
    });
  }
}
