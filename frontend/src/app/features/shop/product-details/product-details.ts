import { Component, inject } from '@angular/core';
import { CurrencyPipe } from '@angular/common';
import { MAT_DIALOG_DATA, MatDialogClose } from '@angular/material/dialog';
import { MatButton, MatIconButton } from '@angular/material/button';
import { MatIcon } from '@angular/material/icon';
import { Product } from '../../../core/models/product';

@Component({
  selector: 'app-product-details',
  imports: [CurrencyPipe, MatDialogClose, MatIconButton, MatIcon],
  templateUrl: './product-details.html',
  styleUrl: './product-details.scss',
})
export class ProductDetails {
  product: Product = inject(MAT_DIALOG_DATA);
}
