import { TestBed } from '@angular/core/testing';
import {
    HttpClientTestingModule,
    HttpTestingController,
} from '@angular/common/http/testing';

import { UserService } from './user.service';
import { UpdateUserRequest, User } from '../models/user';

describe('UserService', () => {
    let service: UserService;
    let httpTestingController: HttpTestingController;

    // Mock data for tests
    const mockUser: User = {
        userId: '1',
        email: 'test@example.com',
        username: 'Test User',
        role: 'CLIENT',
        avatarMediaId: null,
    };

    beforeEach(() => {
        TestBed.configureTestingModule({
            imports: [HttpClientTestingModule],
            providers: [UserService],
        });

        service = TestBed.inject(UserService);
        httpTestingController = TestBed.inject(HttpTestingController);
    });

    afterEach(() => {
        httpTestingController.verify();
    });

    it('should be created', () => {
        expect(service).toBeTruthy();
    });

    describe('#updateProfile', () => {
        it('should send a PATCH request to /users/me and return the updated user', () => {
            const updateUserPayload: UpdateUserRequest = { username: 'Updated' };

            service.updateProfile(updateUserPayload).subscribe((user) => {
                expect(user).toEqual(mockUser);
            });

            const req = httpTestingController.expectOne('/users/me');
            expect(req.request.method).toBe('PATCH');
            expect(req.request.body).toEqual(updateUserPayload);

            req.flush(mockUser);
        });
    });
});