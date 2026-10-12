import { ComponentFixture, TestBed, fakeAsync, flush } from '@angular/core/testing';
import { ANIMATION_MODULE_TYPE } from '@angular/platform-browser/animations';
import { HttpErrorResponse } from '@angular/common/http';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { signal } from '@angular/core';
import { of, Subject, throwError } from 'rxjs';

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
                { provide: ANIMATION_MODULE_TYPE, useValue: 'NoopAnimations' },
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

        it('should preserve a new selection when the initial avatar lookup returns late', () => {
            const lookup = new Subject<ProfileImageResponse>();
            mediaServiceSpy.getProfile.and.returnValue(lookup);
            spyOn(URL, 'createObjectURL').and.returnValue('blob:new-avatar');
            createComponent(mockUserWithAvatar);
            fixture.detectChanges();
            const file = new File(['image'], 'new.png', { type: 'image/png' });
            component.onAvatarSelected({ target: { files: [file] } } as unknown as Event);
            lookup.next(mockProfileResponse);
            expect(component.avatarPreview()).toBe('blob:new-avatar');
            lookup.error(new Error('Late lookup failure'));
            expect(component.avatarPreview()).toBe('blob:new-avatar');
        });

        it('should allow clearing the saved avatar when the latest upload does not match', () => {
            mediaServiceSpy.getProfile.and.returnValue(of({ avatar: { id: 'unsaved', url: 'unsaved.png' } }));
            createComponent(mockUserWithAvatar);
            fixture.detectChanges();
            expect(component.avatarPreview()).toBeNull();
            const remove = fixture.nativeElement.querySelector('.remove-avatar') as HTMLButtonElement;
            expect(remove).not.toBeNull();
            userServiceSpy.updateProfile.and.returnValue(of({ ...mockUser, avatarMediaId: null }));
            remove.click();
            component.onSubmit();
            expect(userServiceSpy.updateProfile).toHaveBeenCalledWith({ avatarMediaId: null });
        });

        it('should not restore an avatar removed before the initial lookup finishes', () => {
            const lookup = new Subject<ProfileImageResponse>();
            mediaServiceSpy.getProfile.and.returnValue(lookup);
            createComponent(mockUserWithAvatar);
            fixture.detectChanges();
            component.removeAvatar();
            lookup.next(mockProfileResponse);
            expect(component.avatarPreview()).toBeNull();
            expect(component.avatarRemoved()).toBeTrue();
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
            const mockFile = new File(['image'], 'avatar.png', { type: 'image/png' });
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

                const mockFile = new File(['image'], 'new-avatar.png', { type: 'image/png' });
                component.onAvatarSelected({ target: { files: [mockFile] } } as unknown as Event);

                component.onSubmit();

                expect(mediaServiceSpy.uploadProfile).toHaveBeenCalledWith(mockFile);
                expect(userServiceSpy.updateProfile).toHaveBeenCalledWith({ avatarMediaId: 'new-media-id' });
                expect(dialogRefSpy.close).toHaveBeenCalledWith(updatedUser);
            });

            it('should reuse an uploaded avatar when saving the profile initially fails', () => {
                const file = new File(['image'], 'avatar.png', { type: 'image/png' });
                mediaServiceSpy.uploadProfile.and.returnValue(of(mockProfileResponse));
                userServiceSpy.updateProfile.and.returnValues(
                    throwError(() => new HttpErrorResponse({ status: 503, error: { message: 'Unavailable', details: {} } })),
                    of({ ...mockUser, avatarMediaId: 'media-1' }),
                );
                component.onAvatarSelected({ target: { files: [file] } } as unknown as Event);
                component.onSubmit();
                expect(component.submitError()).toBe('Unavailable');
                expect(authServiceSpy.currentUser()).toBeNull();
                component.onSubmit();
                expect(mediaServiceSpy.uploadProfile).toHaveBeenCalledTimes(1);
                expect(userServiceSpy.updateProfile).toHaveBeenCalledTimes(2);
                expect(userServiceSpy.updateProfile).toHaveBeenCalledWith({ avatarMediaId: 'media-1' });
            });

            it('should reject GIF and WebP avatars', () => {
                for (const type of ['image/gif', 'image/webp']) {
                    const file = new File(['image'], 'avatar', { type });
                    component.onAvatarSelected({ target: { files: [file] } } as unknown as Event);
                    expect(component.avatarPreview()).toBeNull();
                    expect(component.submitError()).toContain('JPEG or PNG');
                }
            });

            it('should reject duplicate submits and avatar changes while uploading', () => {
                const upload = new Subject<ProfileImageResponse>();
                mediaServiceSpy.uploadProfile.and.returnValue(upload);
                userServiceSpy.updateProfile.and.returnValue(of({ ...mockUser, avatarMediaId: 'media-1' }));
                const file = new File(['image'], 'avatar.png', { type: 'image/png' });
                component.onAvatarSelected({ target: { files: [file] } } as unknown as Event);
                const preview = component.avatarPreview();
                component.onSubmit();
                component.onSubmit();
                component.removeAvatar();
                component.onAvatarSelected({ target: { files: [new File(['other'], 'other.png', { type: 'image/png' })] } } as unknown as Event);
                expect(component.avatarPreview()).toBe(preview);
                expect(mediaServiceSpy.uploadProfile).toHaveBeenCalledTimes(1);
                expect(component.avatarRemoved()).toBeFalse();
                upload.next(mockProfileResponse);
                upload.complete();
                expect(userServiceSpy.updateProfile).toHaveBeenCalledWith({ avatarMediaId: 'media-1' });
            });

            it('should match profile password and username constraints', () => {
                const password = component.profileForm.controls.password;
                password.setValue('');
                expect(password.valid).toBeTrue();
                password.setValue('a'.repeat(72));
                expect(password.valid).toBeTrue();
                password.setValue('a'.repeat(73));
                expect(password.hasError('maxlength')).toBeTrue();
                component.profileForm.controls.username.setValue('Test User');
                expect(component.profileForm.controls.username.hasError('pattern')).toBeTrue();
            });

            it('should show messages with empty details for unsupported request types', () => {
                userServiceSpy.updateProfile.and.returnValue(throwError(() => new HttpErrorResponse({
                    status: 415, error: { message: 'Unsupported request content type', details: {} },
                })));
                component.profileForm.patchValue({ email: 'new@example.com' });
                component.onSubmit();
                expect(component.submitError()).toBe('Unsupported request content type');
                expect(dialogRefSpy.close).not.toHaveBeenCalled();
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
