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
  // private productService = inject(ProductService);
  private dialog = inject(MatDialog);

  products: Product[] = [
    {
      id: '1',
      name: 'Wireless Headphones',
      description: 'Premium noise-cancelling wireless headphones',
      price: 99.99,
      quantity: 25,
      sellerId: 's1',
      createdAt: new Date(),
      updatedAt: new Date(),
    },
    {
      id: '2',
      name: 'Mechanical Keyboard',
      description: 'RGB mechanical gaming keyboard',
      price: 149.99,
      quantity: 15,
      sellerId: 's1',
      createdAt: new Date(),
      updatedAt: new Date(),
    },
    {
      id: '3',
      name: 'USB-C Hub',
      description: '7-in-1 USB-C multiport adapter',
      price: 45.0,
      quantity: 50,
      sellerId: 's2',
      createdAt: new Date(),
      updatedAt: new Date(),
    },
    {
      id: '4',
      name: 'Laptop Stand',
      description: 'Adjustable aluminum laptop stand',
      price: 59.99,
      quantity: 30,
      sellerId: 's2',
      createdAt: new Date(),
      updatedAt: new Date(),
    },
    {
      id: '5',
      name: 'Webcam HD',
      description: '1080p HD webcam with microphone',
      price: 79.99,
      quantity: 40,
      sellerId: 's1',
      createdAt: new Date(),
      updatedAt: new Date(),
    },
    {
      id: '6',
      name: 'Mouse Pad XL',
      description: 'Extended gaming mouse pad',
      price: 24.99,
      quantity: 100,
      sellerId: 's3',
      createdAt: new Date(),
      updatedAt: new Date(),
    },
  ];
  // products = signal<Product[]>([]);

  // ngOnInit() {
  //   this.productService.getAllProduct().subscribe((products) => {
  //     this.products.set(products);
  //   });
  // }

  openProductDetails(product: Product): void {
    this.dialog.open(ProductDetails, {
      data: product,
      width: '500px',
    });
  }
}
