import { HttpErrorResponse } from '@angular/common/http';
import { Component, inject, signal } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { Router, RouterLink } from '@angular/router';
import { catchError, finalize, of, switchMap } from 'rxjs';

import { UserRole } from '../../../core/models/user';
import { AuthService } from '../../../core/services/auth.service';
import { MediaService } from '../../../core/services/media.service';
import { UserService } from '../../../core/services/user.service';

@Component({
  selector: 'app-register',
  imports: [
    ReactiveFormsModule,
    RouterLink,
    MatButtonModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
  ],
  templateUrl: './register.html',
  styleUrl: './register.scss',
})
export class Register {
  private readonly authService = inject(AuthService);
  private readonly mediaService = inject(MediaService);
  private readonly userService = inject(UserService);
  private readonly router = inject(Router);

  readonly isSubmitting = signal(false);
  readonly submitError = signal('');
  readonly avatarUploadWarning = signal('');
  readonly fieldErrors = signal<Record<string, string>>({});
  readonly hidePassword = signal(true);
  readonly avatarPreview = signal<string | null>(null);
  private avatarFile: File | null = null;

  readonly registerForm = new FormGroup({
    role: new FormControl<UserRole>('CLIENT', { nonNullable: true }),
    username: new FormControl('', {
      nonNullable: true,
      validators: [
        Validators.required,
        Validators.minLength(4),
        Validators.maxLength(15),
        Validators.pattern(/^[a-zA-Z0-9_]+$/),
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

  selectRole(role: UserRole): void {
    this.registerForm.controls.role.setValue(role);
    this.registerForm.controls.role.markAsTouched();
  }

  togglePasswordVisibility(): void {
    this.hidePassword.update((value) => !value);
  }

  onAvatarSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0];
    if (!file) {
      return;
    }

    if (!file.type.startsWith('image/')) {
      this.submitError.set('Avatar must be an image file.');
      input.value = '';
      return;
    }

    if (file.size > 2 * 1024 * 1024) {
      this.submitError.set('Avatar must be 2 MB or less.');
      input.value = '';
      return;
    }

    this.submitError.set('');
    this.avatarFile = file;
    this.avatarPreview.set(URL.createObjectURL(file));
    input.value = '';
  }

  clearAvatar(): void {
    this.avatarFile = null;
    this.avatarPreview.set(null);
  }

  onSubmit(): void {
    this.submitError.set('');
    this.avatarUploadWarning.set('');
    this.fieldErrors.set({});

    this.trimStringControls();
    if (this.registerForm.invalid) {
      this.registerForm.markAllAsTouched();
      return;
    }

    this.isSubmitting.set(true);

    this.authService
      .register(this.registerForm.getRawValue())
      .pipe(
        switchMap((user) => {
          if (!this.avatarFile) {
            return of(user);
          }

          return this.mediaService.uploadProfile(this.avatarFile).pipe(
            switchMap((profile) => this.userService.updateProfile({ avatarMediaId: profile.avatar.id })),
            catchError(() => {
              this.avatarUploadWarning.set('Registration succeeded, but avatar upload failed.');
              return of(user);
            }),
          );
        }),
        finalize(() => this.isSubmitting.set(false)),
      )
      .subscribe({
        next: (user) => {
          this.authService.currentUser.set(user);
          this.router.navigateByUrl(user.role === 'SELLER' ? '/seller' : '/shop');
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

    if (field.hasError('required')) {
      return `${fieldName} is required`;
    }
    if (field.hasError('email')) {
      return 'enter a valid email';
    }
    if (field.hasError('minlength')) {
      return `must be at least ${field.errors?.['minlength'].requiredLength} characters`;
    }
    if (field.hasError('maxlength')) {
      return `must be at most ${field.errors?.['maxlength'].requiredLength} characters`;
    }
    if (field.hasError('pattern')) {
      return 'only letters, numbers, and underscores are allowed';
    }

    return '';
  }

  private trimStringControls(): void {
    const { username, email, password } = this.registerForm.controls;
    username.setValue(username.value.trim());
    email.setValue(email.value.trim());
    password.setValue(password.value.trim());
  }

  private handleSubmitError(error: HttpErrorResponse): void {
    if (error.error?.details) {
      const details = error.error.details;
      this.fieldErrors.set(details);
      Object.keys(details).forEach((fieldName) => {
        const control = this.registerForm.get(fieldName);
        control?.setErrors({ backend: details[fieldName] });
        control?.markAsTouched();
      });
      return;
    }

    this.submitError.set(error.error?.message ?? error.error?.error ?? 'Registration failed');
  }
}
