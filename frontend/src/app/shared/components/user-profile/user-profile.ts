import { HttpErrorResponse } from '@angular/common/http';
import { Component, inject, OnInit, signal } from '@angular/core';
import { ReactiveFormsModule, FormGroup, FormControl, Validators } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogClose, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { finalize, switchMap } from 'rxjs';
import { User, UserRole, UpdateUserRequest } from '../../../core/models/user';
import { UserService } from '../../../core/services/user.service';
import { AuthService } from '../../../core/services/auth.service';
import { MediaService } from '../../../core/services/media.service';

@Component({
  selector: 'app-profile-dialog',
  imports: [
    ReactiveFormsModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatButtonModule,
    MatIconModule,
    MatDialogClose,
  ],
  templateUrl: './user-profile.html',
  styleUrl: './user-profile.scss',
})
export class ProfileDialog {
  private userService = inject(UserService);
  private authService = inject(AuthService);
  private mediaService = inject(MediaService);
  private dialogRef = inject(MatDialogRef<ProfileDialog>);

  user: User = inject(MAT_DIALOG_DATA);

  readonly isSubmitting = signal(false);
  readonly submitError = signal('');
  readonly fieldErrors = signal<{ [key: string]: string }>({});
  readonly hidePassword = signal(true); 
  readonly avatarPreview = signal<string | null>(null);
  readonly avatarRemoved = signal(false);
  private avatarFile: File | null = null;

  readonly roles: UserRole[] = ['CLIENT', 'SELLER'];

  readonly profileForm = new FormGroup({
    username: new FormControl(this.user.username, {
      nonNullable: true,
      validators: [Validators.required, Validators.minLength(4), Validators.maxLength(15)],
    }),
    email: new FormControl(this.user.email, {
      nonNullable: true,
      validators: [Validators.required, Validators.email],
    }),
    password: new FormControl('', {
      nonNullable: true,
      validators: [Validators.minLength(6), Validators.maxLength(100)],
    }),
    role: new FormControl<UserRole>(this.user.role, {
      nonNullable: true,
    }),
  });

  ngOnInit(): void {
    if (!this.user.avatarMediaId) {
      return;
    }

    this.mediaService.getProfile(this.user.userId).subscribe({
      next: (profile) => this.avatarPreview.set(profile.avatar.url),
      error: () => this.avatarPreview.set(null),
    });
  }

  togglePasswordVisibility(): void {
    this.hidePassword.update((v) => !v);
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

    this.avatarFile = file;
    this.avatarPreview.set(URL.createObjectURL(file));
    this.avatarRemoved.set(false);
    input.value = '';
  }

  removeAvatar(): void {
    this.avatarFile = null;
    this.avatarPreview.set(null);
    this.avatarRemoved.set(true);
  }

  onSubmit(): void {
    this.fieldErrors.set({});
    this.submitError.set('');

    if (this.profileForm.invalid) {
      return;
    }

    const formValue = this.profileForm.getRawValue();
    const payload: UpdateUserRequest = {};
    const roleChanged = formValue.role !== this.user.role;

    if (formValue.username !== this.user.username) {
      payload.username = formValue.username;
    }
    if (formValue.email !== this.user.email) {
      payload.email = formValue.email;
    }
    if (formValue.password) {
      payload.password = formValue.password;
    }
    if (formValue.role !== this.user.role) {
      payload.role = formValue.role;
    }
    if (this.avatarRemoved()) {
      payload.avatarMediaId = null;
    }

    const hasAvatarUpload = !!this.avatarFile;
    if (!hasAvatarUpload && Object.keys(payload).length === 0) {
      this.dialogRef.close();
      return;
    }

    this.isSubmitting.set(true);

    const request$ = hasAvatarUpload
      ? this.mediaService.uploadProfile(this.avatarFile!).pipe(
        switchMap((profile) => {
          payload.avatarMediaId = profile.avatar.id;
          return this.userService.updateProfile(payload);
        }))
      : this.userService.updateProfile(payload);

    request$
      .pipe(finalize(() => this.isSubmitting.set(false)))
      .subscribe({
        next: (updatedUser) => {
          if (roleChanged) {
            this.dialogRef.close();
            this.authService.logout('/login');
            return;
          }
          this.authService.currentUser.set(updatedUser);
          this.dialogRef.close(updatedUser);
        },
        error: (error: HttpErrorResponse) => this.handleSubmitError(error),
      });
  }

  getErrorMessage(fieldName: string): string {
    const backendError = this.fieldErrors()[fieldName];
    if (backendError) {
      return backendError;
    }

    const field = this.profileForm.get(fieldName);
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
    if (errors['email']) {
      return 'Enter a valid email';
    }
    if (errors['minlength']) {
      return `Minimum ${errors['minlength'].requiredLength} characters`;
    }
    if (errors['maxlength']) {
      return `Maximum ${errors['maxlength'].requiredLength} characters`;
    }

    return '';
  }

  private handleSubmitError(error: HttpErrorResponse): void {
    if (error.error?.details) {
      const details = error.error.details;
      this.fieldErrors.set(details);

      Object.keys(details).forEach((fieldName) => {
        const control = this.profileForm.get(fieldName);
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

    this.submitError.set('Failed to update profile');
  }
}
