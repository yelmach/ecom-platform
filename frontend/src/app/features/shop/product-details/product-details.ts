import { Component, computed, inject, signal } from '@angular/core';
import { CurrencyPipe } from '@angular/common';
import { MAT_DIALOG_DATA, MatDialogClose } from '@angular/material/dialog';
import { MatIconButton } from '@angular/material/button';
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
  private static readonly SWIPE_THRESHOLD_PX = 40;

  private mediaService = inject(MediaService);

  product: Product = inject(MAT_DIALOG_DATA);
  readonly placeholderUrl = `https://placehold.co/400x280/222/666?text=${this.product.name}`;
  readonly imageUrls = signal<string[]>([this.placeholderUrl]);
  readonly activeImageIndex = signal(0);
  readonly currentImageUrl = computed(
    () => this.imageUrls()[this.activeImageIndex()] ?? this.placeholderUrl,
  );
  readonly hasMultipleImages = computed(() => this.imageUrls().length > 1);
  private touchStartX: number | null = null;

  ngOnInit(): void {
    this.mediaService.getProductImages(this.product.id).subscribe({
      next: (response) => {
        const orderedImages = this.mediaService.getOrderedProductImages(
          response,
          this.product.mediaIds,
        );
        const urls = orderedImages.map((image) => image.url);

        if (urls.length) {
          this.imageUrls.set(urls);
          this.activeImageIndex.set(0);
        }
      },
      error: () => {},
    });
  }

  previousImage(): void {
    if (!this.hasMultipleImages()) {
      return;
    }

    const length = this.imageUrls().length;
    this.activeImageIndex.update((index) => (index - 1 + length) % length);
  }

  nextImage(): void {
    if (!this.hasMultipleImages()) {
      return;
    }

    const length = this.imageUrls().length;
    this.activeImageIndex.update((index) => (index + 1) % length);
  }

  goToImage(index: number): void {
    if (index < 0 || index >= this.imageUrls().length) {
      return;
    }
    this.activeImageIndex.set(index);
  }

  onTouchStart(event: TouchEvent): void {
    this.touchStartX = event.changedTouches[0]?.clientX ?? null;
  }

  onTouchEnd(event: TouchEvent): void {
    if (this.touchStartX === null || !this.hasMultipleImages()) {
      this.touchStartX = null;
      return;
    }

    const touchEndX = event.changedTouches[0]?.clientX;
    if (touchEndX === undefined) {
      this.touchStartX = null;
      return;
    }

    const deltaX = touchEndX - this.touchStartX;
    if (Math.abs(deltaX) < ProductDetails.SWIPE_THRESHOLD_PX) {
      this.touchStartX = null;
      return;
    }

    if (deltaX > 0) {
      this.previousImage();
    } else {
      this.nextImage();
    }

    this.touchStartX = null;
  }
}
