import { TestBed } from '@angular/core/testing';
import { ActivatedRouteSnapshot, Router, RouterStateSnapshot, UrlTree } from '@angular/router';
import { Observable, of, throwError } from 'rxjs';

import { productOwnerGuard } from './product-owner.guard';
import { AuthService } from '../services/auth.service';
import { ProductService } from '../services/product.service';
import { User } from '../models/user';
import { Product } from '../models/product';

describe('productOwnerGuard', () => {
  let routerSpy: jasmine.SpyObj<Router>;
  let authServiceSpy: jasmine.SpyObj<AuthService>;
  let productServiceSpy: jasmine.SpyObj<ProductService>;

  const dummyUrlTree = {} as UrlTree;
  
  const mockUser: User = {
    userId: 'seller-123',
    email: 'seller@example.com',
    username: 'Test Seller',
    role: 'SELLER',
    avatarMediaId: null,
  };

  const mockProduct = {
    id: 'prod-1',
    sellerId: 'seller-123',
    name: 'Test Product',
  } as Product;

  beforeEach(() => {
    routerSpy = jasmine.createSpyObj('Router', ['createUrlTree']);
    authServiceSpy = jasmine.createSpyObj('AuthService', ['currentUser']);
    productServiceSpy = jasmine.createSpyObj('ProductService', ['getSingleProduct']);

    routerSpy.createUrlTree.and.returnValue(dummyUrlTree);

    TestBed.configureTestingModule({
      providers: [
        { provide: Router, useValue: routerSpy },
        { provide: AuthService, useValue: authServiceSpy },
        { provide: ProductService, useValue: productServiceSpy },
      ],
    });
  });

  // Helper function to run the guard inside the injection context
  const runGuard = (routeId: string | null) => {
    const route = {
      paramMap: {
        get: (key: string) => routeId,
      },
    } as unknown as ActivatedRouteSnapshot;
    
    const state = {} as RouterStateSnapshot;
    
    return TestBed.runInInjectionContext(() => productOwnerGuard(route, state));
  };

  it('should be created', () => {
    expect(productOwnerGuard).toBeTruthy();
  });

  it('should redirect to /seller synchronously if there is no current user', () => {
    authServiceSpy.currentUser.and.returnValue(null);
    const result = runGuard('prod-1');
    
    expect(result).toBe(dummyUrlTree);
    expect(routerSpy.createUrlTree).toHaveBeenCalledWith(['/seller']);
  });

  it('should redirect to /seller synchronously if there is no product ID in the route', () => {
    authServiceSpy.currentUser.and.returnValue(mockUser);
    const result = runGuard(null);
    
    expect(result).toBe(dummyUrlTree);
    expect(routerSpy.createUrlTree).toHaveBeenCalledWith(['/seller']);
  });

  it('should allow access (return true) if the user is the seller of the product', (done) => {
    authServiceSpy.currentUser.and.returnValue(mockUser);
    productServiceSpy.getSingleProduct.and.returnValue(of(mockProduct));

    const result$ = runGuard('prod-1') as Observable<boolean | UrlTree>;
    result$.subscribe((result) => {
      expect(result).toBe(true);
      expect(routerSpy.createUrlTree).not.toHaveBeenCalled();
      done();
    });
  });

  it('should redirect to /seller if the user is NOT the seller of the product', (done) => {
    const otherUser = { ...mockUser, userId: 'different-seller' };
    authServiceSpy.currentUser.and.returnValue(otherUser);
    productServiceSpy.getSingleProduct.and.returnValue(of(mockProduct));

    const result$ = runGuard('prod-1') as Observable<boolean | UrlTree>;
    result$.subscribe((result) => {
      expect(result).toBe(dummyUrlTree);
      expect(routerSpy.createUrlTree).toHaveBeenCalledWith(['/seller']);
      done();
    });
  });

  it('should redirect to /seller if the product API call fails', (done) => {
    authServiceSpy.currentUser.and.returnValue(mockUser);
    productServiceSpy.getSingleProduct.and.returnValue(throwError(() => new Error('Product not found')));

    const result$ = runGuard('prod-1') as Observable<boolean | UrlTree>;
    result$.subscribe((result) => {
      expect(result).toBe(dummyUrlTree);
      expect(routerSpy.createUrlTree).toHaveBeenCalledWith(['/seller']);
      done();
    });
  });
});