import { HttpErrorResponse } from '@angular/common/http';
import { Component, OnInit, inject, signal } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { ActivatedRoute, Router } from '@angular/router';
import { finalize, of, switchMap } from 'rxjs';

import { MediaImage } from '../../../core/models/media';
import { Product, ProductFormData, ProductUpdateData } from '../../../core/models/product';
import { MediaService } from '../../../core/services/media.service';
import { ProductService } from '../../../core/services/product.service';

interface ProductImagePreview {
  file: File | null;
  url: string;
  mediaId?: string;
}

@Component({
  selector: 'app-new-product',
  imports: [
    ReactiveFormsModule,
    MatButtonModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
  ],
  templateUrl: './new-product.html',
  styleUrl: './new-product.scss',
})
export class NewProduct implements OnInit {
  private static readonly MAX_IMAGES = 5;
  private static readonly MAX_IMAGE_SIZE_BYTES = 2 * 1024 * 1024;

  private readonly productService = inject(ProductService);
  private readonly mediaService = inject(MediaService);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);

  readonly isEditMode = signal(false);
  readonly isSubmitting = signal(false);
  readonly isDragging = signal(false);
  readonly submitError = signal('');
  readonly imageError = signal('');
  readonly fieldErrors = signal<Record<string, string>>({});
  readonly images = signal<ProductImagePreview[]>([]);

  private productId: string | null = null;
  private initialProduct: Product | null = null;
  private initialMediaIds: string[] = [];

  readonly productForm = new FormGroup({
    name: new FormControl('', { nonNullable: true, validators: [Validators.required] }),
    description: new FormControl('', { nonNullable: true, validators: [Validators.required] }),
    price: new FormControl(0, { nonNullable: true, validators: [Validators.required, Validators.min(0.01)] }),
    quantity: new FormControl(0, { nonNullable: true, validators: [Validators.required, Validators.min(1)] }),
  });

  ngOnInit(): void {
    this.productId = this.route.snapshot.paramMap.get('id');
    this.isEditMode.set(!!this.productId);

    if (this.productId) {
      this.loadProduct(this.productId);
    }
  }

  onFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    this.addFiles(Array.from(input.files ?? []));
    input.value = '';
  }

  onDragOver(event: DragEvent): void {
    event.preventDefault();
    this.isDragging.set(true);
  }

  onDragLeave(event: DragEvent): void {
    event.preventDefault();
    this.isDragging.set(false);
  }

  onDrop(event: DragEvent): void {
    event.preventDefault();
    this.isDragging.set(false);
    this.addFiles(Array.from(event.dataTransfer?.files ?? []));
  }

  removeImage(index: number): void {
    this.images.update((images) => images.filter((_, imageIndex) => imageIndex !== index));
  }

  isSubmitDisabled(): boolean {
    return this.isSubmitting() || this.productForm.invalid || (this.isEditMode() && !this.hasChanges());
  }

  onSubmit(): void {
    this.submitError.set('');
    this.fieldErrors.set({});

    if (this.productForm.invalid) {
      this.productForm.markAllAsTouched();
      return;
    }

    this.isSubmitting.set(true);

    if (this.isEditMode() && this.productId) {
      this.submitEdit(this.productId);
      return;
    }

    this.submitCreate();
  }

  cancel(): void {
    this.router.navigateByUrl('/seller');
  }

  getErrorMessage(fieldName: string): string {
    const backendError = this.fieldErrors()[fieldName];
    if (backendError) {
      return backendError;
    }

    const field = this.productForm.get(fieldName);
    if (!field || field.valid || field.untouched) {
      return '';
    }

    if (field.hasError('required')) {
      return `${fieldName} is required`;
    }
    if (field.hasError('min')) {
      return `${fieldName} is too low`;
    }

    return '';
  }

  private loadProduct(productId: string): void {
    this.productService.getSingleProduct(productId).subscribe({
      next: (product) => {
        this.initialProduct = product;
        this.initialMediaIds = [...product.mediaIds];
        this.productForm.setValue({
          name: product.name,
          description: product.description,
          price: product.price,
          quantity: product.quantity,
        });
        this.loadExistingImages(product);
      },
      error: () => this.submitError.set('Failed to load product'),
    });
  }

  private loadExistingImages(product: Product): void {
    this.mediaService.getProductImages(product.id).subscribe({
      next: (response) => {
        const orderedImages = this.mediaService.getOrderedProductImages(response, product.mediaIds);
        this.images.set(
          orderedImages.map((image: MediaImage) => ({
            mediaId: image.id,
            url: image.url,
            file: null,
          })),
        );
      },
      error: () => this.images.set([]),
    });
  }

  private submitCreate(): void {
    const payload: ProductFormData = {
      ...this.productForm.getRawValue(),
      mediaIds: [],
    };
    const files = this.newImageFiles();

    this.productService
      .createProduct(payload)
      .pipe(
        switchMap((product) => {
          if (!files.length) {
            return of(product);
          }
          return this.mediaService.uploadProductImages(product.id, files).pipe(
            switchMap((response) =>
              this.productService.updateProduct(product.id, {
                mediaIds: response.images.map((image) => image.id),
              }),
            ),
          );
        }),
        finalize(() => this.isSubmitting.set(false)),
      )
      .subscribe({
        next: () => this.router.navigateByUrl('/seller'),
        error: (error: HttpErrorResponse) => this.handleSubmitError(error),
      });
  }

  private submitEdit(productId: string): void {
    const files = this.newImageFiles();
    const existingMediaIds = this.images()
      .map((image) => image.mediaId)
      .filter((mediaId): mediaId is string => !!mediaId);

    const updateWithMediaIds = (newMediaIds: string[] = []) => {
      const payload: ProductUpdateData = {
        ...this.productForm.getRawValue(),
        mediaIds: [...existingMediaIds, ...newMediaIds],
      };
      return this.productService.updateProduct(productId, payload);
    };

    const request$ = files.length
      ? this.mediaService.uploadProductImages(productId, files).pipe(
          switchMap((response) => updateWithMediaIds(response.images.map((image) => image.id))),
        )
      : updateWithMediaIds();

    request$.pipe(finalize(() => this.isSubmitting.set(false))).subscribe({
      next: () => this.router.navigateByUrl('/seller'),
      error: (error: HttpErrorResponse) => this.handleSubmitError(error),
    });
  }

  private addFiles(files: File[]): void {
    this.imageError.set('');

    const acceptedImages = [...this.images()];
    for (const file of files) {
      if (acceptedImages.length >= NewProduct.MAX_IMAGES || !this.isValidImage(file)) {
        this.imageError.set('Only PNG/JPG/GIF/WEBP images up to 2MB are allowed, max 5 files.');
        continue;
      }

      acceptedImages.push({
        file,
        url: URL.createObjectURL(file),
      });
    }

    this.images.set(acceptedImages);
  }

  private isValidImage(file: File): boolean {
    const allowedTypes = ['image/png', 'image/jpeg', 'image/gif', 'image/webp'];
    return allowedTypes.includes(file.type) && file.size <= NewProduct.MAX_IMAGE_SIZE_BYTES;
  }

  private newImageFiles(): File[] {
    return this.images()
      .map((image) => image.file)
      .filter((file): file is File => !!file);
  }

  private hasChanges(): boolean {
    if (!this.initialProduct) {
      return true;
    }

    const formValue = this.productForm.getRawValue();
    const formChanged =
      formValue.name !== this.initialProduct.name ||
      formValue.description !== this.initialProduct.description ||
      formValue.price !== this.initialProduct.price ||
      formValue.quantity !== this.initialProduct.quantity;

    const currentMediaIds = this.images()
      .map((image) => image.mediaId)
      .filter((mediaId): mediaId is string => !!mediaId);

    return (
      formChanged ||
      this.newImageFiles().length > 0 ||
      currentMediaIds.join('|') !== this.initialMediaIds.join('|')
    );
  }

  private handleSubmitError(error: HttpErrorResponse): void {
    if (error.error?.details) {
      const details = error.error.details;
      this.fieldErrors.set(details);
      Object.keys(details).forEach((fieldName) => {
        const control = this.productForm.get(fieldName);
        control?.setErrors({ backend: details[fieldName] });
        control?.markAsTouched();
      });
      return;
    }

    if (error.status === 403) {
      this.submitError.set('Seller permission is required to manage products.');
      return;
    }

    this.submitError.set(error.error?.message ?? error.error?.error ?? 'Failed to save product');
  }
}
