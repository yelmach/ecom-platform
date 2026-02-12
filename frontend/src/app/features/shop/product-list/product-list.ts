import { Component, inject, OnInit, signal } from '@angular/core';
import { Product } from '../../../core/models/product';
import { ProductService } from '../../../core/services/product.service';
import { CurrencyPipe } from '@angular/common';
import { MatButton } from '@angular/material/button';
import { MatIcon } from '@angular/material/icon';
import { MatDialog } from '@angular/material/dialog';
import { ProductDetails } from '../product-details/product-details';

@Component({
  selector: 'app-product-list',
  imports: [CurrencyPipe, MatButton, MatIcon],
  templateUrl: './product-list.html',
  styleUrl: './product-list.scss',
})
export class ProductList {
  private productService = inject(ProductService);
  private dialog = inject(MatDialog);

  products = signal<Product[]>([]);

  ngOnInit() {
    this.productService.getAllProduct().subscribe((page) => {
      this.products.set(page.content);
    });
  }

  openProductDetails(product: Product): void {
    this.dialog.open(ProductDetails, {
      data: product,
      width: '500px',
    });
  }
}
