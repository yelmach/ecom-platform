import { Component, inject, OnInit, signal } from '@angular/core';
import { Router } from '@angular/router';
import { CurrencyPipe } from '@angular/common';
import { MatButton, MatIconButton } from '@angular/material/button';
import { MatIcon } from '@angular/material/icon';
import { MatMenuModule } from '@angular/material/menu';
import { MatDialog } from '@angular/material/dialog';
import { ProductService } from '../../../core/services/product.service';
import { Product } from '../../../core/models/product';
import { ProductDetails } from '../../shop/product-details/product-details';

@Component({
  selector: 'app-dashboard',
  imports: [CurrencyPipe, MatButton, MatIconButton, MatIcon, MatMenuModule],
  templateUrl: './dashboard.html',
  styleUrl: './dashboard.scss',
})
export class Dashboard implements OnInit {
  private productService = inject(ProductService);
  private router = inject(Router);
  private dialog = inject(MatDialog);

  products = signal<Product[]>([]);
  totalProducts = signal(0);

  ngOnInit() {
    this.loadProducts();
  }

  loadProducts() {
    this.productService.getMyProducts().subscribe((page) => {
      this.products.set(page.content);
      this.totalProducts.set(page.totalElements);
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
}
