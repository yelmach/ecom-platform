import { ComponentFixture, TestBed, fakeAsync, flush } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { MatDialog } from '@angular/material/dialog';
import { signal } from '@angular/core';
import { of, throwError } from 'rxjs';

import { Header } from './header';
import { AuthService } from '../../../core/services/auth.service';
import { MediaService } from '../../../core/services/media.service';
import { User } from '../../../core/models/user';
import { ProfileImageResponse } from '../../../core/models/media';
import { ProfileDialog } from '../user-profile/user-profile';

describe('Header Component', () => {
  let component: Header;
  let fixture: ComponentFixture<Header>;
  let authServiceSpy: jasmine.SpyObj<AuthService>;
  let mediaServiceSpy: jasmine.SpyObj<MediaService>;
  let dialogSpy: jasmine.SpyObj<MatDialog>;

  const mockUser: User = {
    userId: 'user-1',
    email: 'test@test.com',
    username: 'TestUser',
    role: 'CLIENT',
    avatarMediaId: null,
  };

  const mockProfileResponse: ProfileImageResponse = {
    avatar: { id: 'media-1', url: 'http://example.com/avatar.jpg' },
  };

  beforeEach(async () => {
    // Create real signals for our AuthService spy so the component can read and write to them
    const currentUserSignal = signal<User | null>(null);
    const isAuthenticatedSignal = signal<boolean>(false);

    authServiceSpy = jasmine.createSpyObj('AuthService', ['logout'], {
      currentUser: currentUserSignal,
      isAuthenticated: isAuthenticatedSignal,
    });

    mediaServiceSpy = jasmine.createSpyObj('MediaService', ['getProfile']);
    dialogSpy = jasmine.createSpyObj('MatDialog', ['open']);

    await TestBed.configureTestingModule({
      imports: [Header],
      providers: [
        provideRouter([]), // Required for RouterLink
        { provide: AuthService, useValue: authServiceSpy },
        { provide: MediaService, useValue: mediaServiceSpy },
        { provide: MatDialog, useValue: dialogSpy },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(Header);
    component = fixture.componentInstance;
  });

  it('should create the component', () => {
    fixture.detectChanges();
    expect(component).toBeTruthy();
  });

  describe('Avatar Effect', () => {
    it('should set avatarUrl to null if no user is logged in', fakeAsync(() => {
      authServiceSpy.currentUser.set(null);
      fixture.detectChanges(); // Triggers the effect
      flush(); // Resolves any async effect scheduling

      expect(component.avatarUrl()).toBeNull();
      expect(mediaServiceSpy.getProfile).not.toHaveBeenCalled();
    }));

    it('should set avatarUrl to null if user has no avatarMediaId', fakeAsync(() => {
      authServiceSpy.currentUser.set(mockUser);
      fixture.detectChanges();
      flush();

      expect(component.avatarUrl()).toBeNull();
      expect(mediaServiceSpy.getProfile).not.toHaveBeenCalled();
    }));

    it('should fetch and set avatarUrl if user has avatarMediaId', fakeAsync(() => {
      const userWithAvatar = { ...mockUser, avatarMediaId: 'media-1' };
      mediaServiceSpy.getProfile.and.returnValue(of(mockProfileResponse));

      authServiceSpy.currentUser.set(userWithAvatar);
      fixture.detectChanges();
      flush();

      expect(mediaServiceSpy.getProfile).toHaveBeenCalledWith('user-1');
      expect(component.avatarUrl()).toBe('http://example.com/avatar.jpg');
    }));

    it('should handle getProfile API error gracefully and set avatarUrl to null', fakeAsync(() => {
      const userWithAvatar = { ...mockUser, avatarMediaId: 'media-1' };
      mediaServiceSpy.getProfile.and.returnValue(throwError(() => new Error('Network Error')));

      authServiceSpy.currentUser.set(userWithAvatar);
      fixture.detectChanges();
      flush();

      expect(mediaServiceSpy.getProfile).toHaveBeenCalledWith('user-1');
      expect(component.avatarUrl()).toBeNull();
    }));
  });

  describe('Getters & User Actions', () => {
    it('should correctly identify if the current user is a SELLER', () => {
      authServiceSpy.currentUser.set(mockUser); // Role is CLIENT
      expect(component.isSeller).toBeFalse();

      authServiceSpy.currentUser.set({ ...mockUser, role: 'SELLER' });
      expect(component.isSeller).toBeTrue();
    });

    it('should call authService.logout when logout is triggered', () => {
      component.logout();
      expect(authServiceSpy.logout).toHaveBeenCalled();
    });

    it('should open profile dialog and update user signal if dialog returns data', () => {
      const updatedUser = { ...mockUser, username: 'UpdatedName' };
      const dialogRefSpy = jasmine.createSpyObj('MatDialogRef', ['afterClosed']);
      
      dialogRefSpy.afterClosed.and.returnValue(of(updatedUser));
      dialogSpy.open.and.returnValue(dialogRefSpy);
      authServiceSpy.currentUser.set(mockUser);

      component.openProfile();

      expect(dialogSpy.open).toHaveBeenCalledWith(ProfileDialog, { data: mockUser, width: '550px' });
      expect(authServiceSpy.currentUser()).toEqual(updatedUser);
    });
  });
});