import { HttpErrorResponse } from '@angular/common/http';
import { Component, inject, signal } from '@angular/core';
import { ReactiveFormsModule, FormGroup, FormControl, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatIcon, MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { finalize } from 'rxjs';
import { ProductService } from '../../../core/services/product.service';

interface ImagePreview {
  file: File;
  url: string;
}

@Component({
  selector: 'app-new-product',
  imports: [ReactiveFormsModule, MatFormFieldModule, MatInputModule, MatIconModule, MatButtonModule, MatIcon],
  templateUrl: './new-product.html',
  styleUrl: './new-product.scss',
})
export class NewProduct {
  private productService = inject(ProductService);
  private router = inject(Router);

  readonly isSubmitting = signal(false);
  readonly submitError = signal('');
  readonly fieldErrors = signal<{ [key: string]: string }>({});
  readonly images = signal<ImagePreview[]>([]);
  readonly isDragging = signal(false);

  readonly productForm = new FormGroup({
    name: new FormControl('', {
      nonNullable: true,
      validators: [Validators.required],
    }),
    description: new FormControl('', {
      nonNullable: true,
      validators: [Validators.required],
    }),
    price: new FormControl<number | null>(null, {
      validators: [Validators.required, Validators.min(0.01)],
    }),
    quantity: new FormControl<number | null>(null, {
      validators: [Validators.required, Validators.min(1)],
    }),
  });

  onFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    if (input.files) {
      this.addFiles(Array.from(input.files));
      input.value = '';
    }
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
    if (event.dataTransfer?.files) {
      const files = Array.from(event.dataTransfer.files).filter((f) => f.type.startsWith('image/'));
      this.addFiles(files);
    }
  }

  removeImage(index: number): void {
    const updated = this.images().filter((_, i) => i !== index);
    this.images.set(updated);
  }

  onSubmit(): void {
    this.fieldErrors.set({});
    this.submitError.set('');

    if (this.productForm.invalid) {
      return;
    }

    const formValue = this.productForm.getRawValue();
    const payload = {
      name: formValue.name,
      description: formValue.description,
      price: formValue.price!,
      quantity: formValue.quantity!,
      mediaIds: [] as String[],
    };

    this.isSubmitting.set(true);

    this.productService
      .createProduct(payload)
      .pipe(finalize(() => this.isSubmitting.set(false)))
      .subscribe({
        next: () => {
          void this.router.navigateByUrl('/seller');
        },
        error: (error: HttpErrorResponse) => this.handleSubmitError(error),
      });
  }

  cancel(): void {
    void this.router.navigateByUrl('/seller');
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

    const errors = field.errors;
    if (!errors) {
      return '';
    }

    if (errors['required']) {
      return `${fieldName} is required`;
    }
    if (errors['min']) {
      return `${fieldName} must be greater than ${errors['min'].min}`;
    }

    return '';
  }

  private addFiles(files: File[]): void {
    const newPreviews = files.map((file) => ({
      file,
      url: URL.createObjectURL(file),
    }));
    this.images.update((current) => [...current, ...newPreviews]);
  }

  private handleSubmitError(error: HttpErrorResponse): void {
    if (error.error?.details) {
      const details = error.error.details;
      this.fieldErrors.set(details);

      Object.keys(details).forEach((fieldName) => {
        const control = this.productForm.get(fieldName);
        if (control) {
          control.setErrors({ backend: details[fieldName] });
          control.markAsTouched();
        }
      });
      return;
    }

    if (error.error?.message) {
      this.submitError.set(error.error.message);
      return;
    }

    this.submitError.set('Failed to create product');
  }
}
