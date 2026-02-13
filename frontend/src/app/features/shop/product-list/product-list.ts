import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { Product } from '../../../core/models/product';
import { ProductService } from '../../../core/services/product.service';
import { CurrencyPipe } from '@angular/common';
import { MatButton } from '@angular/material/button';
import { MatIcon } from '@angular/material/icon';
import { MatDialog } from '@angular/material/dialog';
import { MatProgressSpinner } from '@angular/material/progress-spinner';
import { ProductDetails } from '../product-details/product-details';
import { InfiniteScrollDirective } from '../../../shared/directives/infinite-scroll.directive';

@Component({
  selector: 'app-product-list',
  imports: [CurrencyPipe, MatButton, MatIcon, MatProgressSpinner, InfiniteScrollDirective],
  templateUrl: './product-list.html',
  styleUrl: './product-list.scss',
})
export class ProductList implements OnInit {
  private productService = inject(ProductService);
  private dialog = inject(MatDialog);

  products = signal<Product[]>([]);
  currentPage = signal(0);
  isLast = signal(false);
  loading = signal(false);

  canLoadMore = computed(() => !this.loading() && !this.isLast());

  ngOnInit(): void {
    this.loadProducts();
  }

  loadProducts(): void {
    if (this.loading() || this.isLast()) return;

    this.loading.set(true);
    this.productService.getAllProduct(this.currentPage(), 10).subscribe({
      next: (page) => {
        this.products.update((current) => [...current, ...page.content]);
        this.isLast.set(page.last);
        this.currentPage.update((p) => p + 1);
        this.loading.set(false);
      },
      error: () => {
        this.loading.set(false);
      },
    });
  }

  openProductDetails(product: Product): void {
    this.dialog.open(ProductDetails, {
      data: product,
      width: '550px',
    });
  }
}
