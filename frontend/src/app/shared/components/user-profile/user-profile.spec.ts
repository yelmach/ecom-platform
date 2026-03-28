import { ComponentFixture, TestBed, fakeAsync, flush } from '@angular/core/testing';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { HttpErrorResponse } from '@angular/common/http';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { signal } from '@angular/core';
import { of, throwError } from 'rxjs';

import { ProfileDialog } from './user-profile';
import { UserService } from '../../../core/services/user.service';
import { AuthService } from '../../../core/services/auth.service';
import { MediaService } from '../../../core/services/media.service';
import { User } from '../../../core/models/user';
import { ProfileImageResponse } from '../../../core/models/media';

describe('ProfileDialog Component', () => {
    let component: ProfileDialog;
    let fixture: ComponentFixture<ProfileDialog>;
    let userServiceSpy: jasmine.SpyObj<UserService>;
    let authServiceSpy: jasmine.SpyObj<AuthService>;
    let mediaServiceSpy: jasmine.SpyObj<MediaService>;
    let dialogRefSpy: jasmine.SpyObj<MatDialogRef<ProfileDialog>>;
    let dialogData: User;

    const mockUser: User = {
        userId: 'user-1',
        username: 'TestUser',
        email: 'test@test.com',
        role: 'CLIENT',
        avatarMediaId: null,
    };

    const mockUserWithAvatar: User = {
        ...mockUser,
        avatarMediaId: 'media-1',
    };

    const mockProfileResponse: ProfileImageResponse = {
        avatar: { id: 'media-1', url: 'http://example.com/avatar.jpg' },
    };

    // Helper to create component with specific user data for each test group
    const createComponent = (user: User) => {
        dialogData = { ...user };
        fixture = TestBed.createComponent(ProfileDialog);
        component = fixture.componentInstance;
    };

    beforeEach(async () => {
        userServiceSpy = jasmine.createSpyObj('UserService', ['updateProfile']);
        authServiceSpy = jasmine.createSpyObj('AuthService', ['logout'], {
            currentUser: signal<User | null>(null), // Use a real signal for mocking
        });
        mediaServiceSpy = jasmine.createSpyObj('MediaService', ['getProfile', 'uploadProfile']);
        dialogRefSpy = jasmine.createSpyObj('MatDialogRef', ['close']);
        
        dialogData = { ...mockUser };

        await TestBed.configureTestingModule({
            imports: [ProfileDialog],
            providers: [
                { provide: UserService, useValue: userServiceSpy },
                { provide: AuthService, useValue: authServiceSpy },
                { provide: MediaService, useValue: mediaServiceSpy },
                { provide: MatDialogRef, useValue: dialogRefSpy },
                { provide: MAT_DIALOG_DATA, useFactory: () => dialogData },
                provideNoopAnimations(),
            ],
        }).compileComponents();
    });

    it('should create the component', () => {
        createComponent(mockUser);
        fixture.detectChanges();
        expect(component).toBeTruthy();
    });

    describe('Initialization', () => {
        it('should not fetch avatar if user has no avatarMediaId', () => {
            createComponent(mockUser);
            fixture.detectChanges(); // ngOnInit
            expect(mediaServiceSpy.getProfile).not.toHaveBeenCalled();
        });

        it('should fetch and display avatar on init if user has avatarMediaId', fakeAsync(() => {
            mediaServiceSpy.getProfile.and.returnValue(of(mockProfileResponse));
            createComponent(mockUserWithAvatar);
            fixture.detectChanges();
            flush(); // Resolve getProfile observable

            expect(mediaServiceSpy.getProfile).toHaveBeenCalledWith(mockUserWithAvatar.userId);
            expect(component.avatarPreview()).toBe('http://example.com/avatar.jpg');
        }));
    });

    describe('Form and UI Interactions', () => {
        beforeEach(() => {
            createComponent(mockUser);
            fixture.detectChanges();
        });

        it('should initialize form with user data', () => {
            expect(component.profileForm.value.username).toBe(mockUser.username);
            expect(component.profileForm.value.email).toBe(mockUser.email);
        });

        it('should handle valid avatar selection', () => {
            spyOn(URL, 'createObjectURL').and.returnValue('blob:mock-url');
            const mockFile = new File([''], 'avatar.png', { type: 'image/png' });
            const mockEvent = { target: { files: [mockFile] } } as unknown as Event;

            component.onAvatarSelected(mockEvent);

            expect(component.avatarPreview()).toBe('blob:mock-url');
            expect(component.avatarRemoved()).toBeFalse();
        });

        it('should remove avatar and set flag', () => {
            component.avatarPreview.set('some-url');
            component.removeAvatar();
            expect(component.avatarPreview()).toBeNull();
            expect(component.avatarRemoved()).toBeTrue();
        });
    });

    describe('Submission Logic', () => {
        describe('with default user', () => {
            beforeEach(() => {
                createComponent(mockUser);
                fixture.detectChanges();
            });

            it('should close dialog without action if no changes are made', () => {
                component.onSubmit();
                expect(userServiceSpy.updateProfile).not.toHaveBeenCalled();
                expect(dialogRefSpy.close).toHaveBeenCalledWith();
            });

            it('should submit only changed username', () => {
                const updatedUser = { ...mockUser, username: 'NewName' };
                userServiceSpy.updateProfile.and.returnValue(of(updatedUser));
                component.profileForm.patchValue({ username: 'NewName' });

                component.onSubmit();

                expect(userServiceSpy.updateProfile).toHaveBeenCalledWith({ username: 'NewName' });
                expect(dialogRefSpy.close).toHaveBeenCalledWith(updatedUser);
            });

            it('should submit new avatar and update profile', () => {
                const updatedUser = { ...mockUser, avatarMediaId: 'new-media-id' };
                mediaServiceSpy.uploadProfile.and.returnValue(of({ avatar: { id: 'new-media-id', url: '' } }));
                userServiceSpy.updateProfile.and.returnValue(of(updatedUser));

                const mockFile = new File([''], 'new-avatar.png', { type: 'image/png' });
                component.onAvatarSelected({ target: { files: [mockFile] } } as unknown as Event);

                component.onSubmit();

                expect(mediaServiceSpy.uploadProfile).toHaveBeenCalledWith(mockFile);
                expect(userServiceSpy.updateProfile).toHaveBeenCalledWith({ avatarMediaId: 'new-media-id' });
                expect(dialogRefSpy.close).toHaveBeenCalledWith(updatedUser);
            });

            it('should logout user if role is changed', () => {
                const updatedUser = { ...mockUser, role: 'SELLER' as const };
                userServiceSpy.updateProfile.and.returnValue(of(updatedUser));
                component.profileForm.patchValue({ role: 'SELLER' });

                component.onSubmit();

                expect(userServiceSpy.updateProfile).toHaveBeenCalledWith({ role: 'SELLER' });
                expect(dialogRefSpy.close).toHaveBeenCalled();
                expect(authServiceSpy.logout).toHaveBeenCalledWith('/login');
            });

            it('should handle backend field errors', () => {
                const errorResponse = new HttpErrorResponse({
                    error: { details: { email: 'Email is already taken' } },
                    status: 400,
                });
                userServiceSpy.updateProfile.and.returnValue(throwError(() => errorResponse));
                component.profileForm.patchValue({ email: 'taken@email.com' });

                component.onSubmit();

                expect(component.fieldErrors()['email']).toBe('Email is already taken');
                expect(dialogRefSpy.close).not.toHaveBeenCalled();
            });
        });

        describe('with user having an avatar', () => {
            beforeEach(() => {
                mediaServiceSpy.getProfile.and.returnValue(of(mockProfileResponse));
                createComponent(mockUserWithAvatar);
                fixture.detectChanges();
            });

            it('should remove avatar by setting avatarMediaId to null', () => {
                const updatedUser = { ...mockUserWithAvatar, avatarMediaId: null };
                userServiceSpy.updateProfile.and.returnValue(of(updatedUser));

                component.removeAvatar();
                component.onSubmit();

                expect(userServiceSpy.updateProfile).toHaveBeenCalledWith({ avatarMediaId: null });
                expect(dialogRefSpy.close).toHaveBeenCalledWith(updatedUser);
            });
        });
    });
});