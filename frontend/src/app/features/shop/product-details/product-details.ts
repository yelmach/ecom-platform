import { Component, inject, signal } from '@angular/core';
import { CurrencyPipe } from '@angular/common';
import { MAT_DIALOG_DATA, MatDialogClose } from '@angular/material/dialog';
import { MatButton, MatIconButton } from '@angular/material/button';
import { MatIcon } from '@angular/material/icon';
import { Product } from '../../../core/models/product';
import { MediaService } from '../../../core/services/media.service';

@Component({
  selector: 'app-product-details',
  imports: [CurrencyPipe, MatDialogClose, MatIconButton, MatIcon],
  templateUrl: './product-details.html',
  styleUrl: './product-details.scss',
})
export class ProductDetails {
  private mediaService = inject(MediaService);

  product: Product = inject(MAT_DIALOG_DATA);
  imageUrl = signal<string>(`https://placehold.co/400x280/222/666?text=${this.product.name}`);

  ngOnInit(): void {
    this.mediaService.getProductImages(this.product.id).subscribe({
      next: (response) => {
        const resolvedUrl = this.mediaService.getPrimaryProductImageUrl(response, this.product.mediaIds);

        if (resolvedUrl) {
          this.imageUrl.set(resolvedUrl);
        }
      },
      error: () => {},
    });
  }
}
