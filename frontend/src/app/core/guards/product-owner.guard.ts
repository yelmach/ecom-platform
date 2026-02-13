import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { map, catchError, of } from 'rxjs';
import { AuthService } from '../services/auth.service';
import { ProductService } from '../services/product.service';

export const productOwnerGuard: CanActivateFn = (route) => {
  const authService = inject(AuthService);
  const productService = inject(ProductService);
  const router = inject(Router);

  const productId = route.paramMap.get('id');
  const user = authService.currentUser();

  if (!user || !productId) {
    return router.createUrlTree(['/seller']);
  }

  return productService.getSingleProduct(productId).pipe(
    map((product) => {
      if (product.sellerId === user.userId) {
        return true;
      }
      return router.createUrlTree(['/seller']);
    }),
    catchError(() => of(router.createUrlTree(['/seller']))),
  );
};
