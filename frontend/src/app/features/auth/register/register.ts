import { HttpErrorResponse } from '@angular/common/http';
import { Component, inject, signal } from '@angular/core';
import { ReactiveFormsModule, FormGroup, FormControl, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { finalize } from 'rxjs';
import { RegisterRequest } from '../../../core/models/auth';
import { AuthService } from '../../../core/services/auth.service';
import { UserRole } from '../../../core/models/user';

@Component({
  selector: 'app-register',
  standalone: true,
  imports: [
    ReactiveFormsModule,
    RouterLink,
    MatFormFieldModule,
    MatInputModule,
    MatIconModule,
    MatButtonModule,
  ],
  templateUrl: './register.html',
  styleUrl: './register.scss',
})
export class Register {
  private authService = inject(AuthService);
  private router = inject(Router);

  readonly registerForm = new FormGroup({
    role: new FormControl<UserRole>('CLIENT', { nonNullable: true }),
    username: new FormControl('', {
      nonNullable: true,
      validators: [
        Validators.required,
        Validators.minLength(4),
        Validators.maxLength(15),
        Validators.pattern(/^[a-zA-Z0-9\s]+$/),
      ],
    }),
    email: new FormControl('', {
      nonNullable: true,
      validators: [Validators.required, Validators.email],
    }),
    password: new FormControl('', {
      nonNullable: true,
      validators: [Validators.required, Validators.minLength(6)],
    }),
  });

  readonly hidePassword = signal(true);
  readonly isSubmitting = signal(false);
  readonly submitError = signal('');
  fieldErrors = signal<{ [key: string]: string }>({});

  selectRole(role: UserRole): void {
    this.registerForm.controls.role.setValue(role);
  }

  togglePasswordVisibility(): void {
    this.hidePassword.update((hide) => !hide);
  }

  onSubmit(): void {
    this.fieldErrors.set({});
    this.submitError.set('');

    Object.keys(this.registerForm.controls).forEach((key) => {
      const control = this.registerForm.get(key);
      if (control && typeof control.value === 'string' && key !== 'password') {
        control.setValue(control.value.trim());
      }
    });

    if (this.registerForm.invalid) {
      return;
    }

    this.isSubmitting.set(true);

    const payload: RegisterRequest = this.registerForm.getRawValue();

    this.authService
      .register(payload)
      .pipe(finalize(() => this.isSubmitting.set(false)))
      .subscribe({
        next: (user) => {
          const redirectPath = user.role === 'SELLER' ? '/seller' : '/shop';
          void this.router.navigateByUrl(redirectPath);
        },
        error: (error: HttpErrorResponse) => this.handleSubmitError(error),
      });
  }

  getErrorMessage(fieldName: string): string {
    const backendError = this.fieldErrors()[fieldName];
    if (backendError) {
      return backendError;
    }

    const field = this.registerForm.get(fieldName);

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

    if (errors['minlength']) {
      const minLength = errors['minlength'].requiredLength;
      return `must be at least ${minLength} characters`;
    }

    if (errors['maxlength']) {
      const maxLength = errors['maxlength'].requiredLength;
      return `must not exceed ${maxLength} characters`;
    }

    if (errors['email']) {
      return 'enter a valid email';
    }

    if (errors['pattern']) {
      return 'only contain letters, numbers';
    }

    return '';
  }

  private handleSubmitError(error: HttpErrorResponse): void {
    if (error.error?.details) {
      const details = error.error.details;
      this.fieldErrors.set(details);

      Object.keys(details).forEach((fieldName) => {
        const control = this.registerForm.get(fieldName);
        if (control) {
          control.setErrors({ backend: details[fieldName] });
          control.markAsTouched();
        }
      });
      return;
    }

    if (error.error?.message) {
      const message = error.error.message;

      this.submitError.set(message);

      return;
    }

    if (error.error?.error) {
      this.submitError.set(error.error.error);
    } else {
      this.submitError.set('Registration failed.');
    }
  }
}
