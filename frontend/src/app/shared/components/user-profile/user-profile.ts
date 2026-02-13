import { Component, inject } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialogClose } from '@angular/material/dialog';
import { MatIconButton } from '@angular/material/button';
import { MatIcon } from '@angular/material/icon';
import { User } from '../../../core/models/user';

@Component({
  selector: 'app-profile-dialog',
  imports: [MatDialogClose, MatIconButton, MatIcon],
  templateUrl: './user-profile.html',
  styleUrl: './user-profile.scss',
})
export class ProfileDialog {
  user: User = inject(MAT_DIALOG_DATA);
}
