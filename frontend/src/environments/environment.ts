export const environment = {
  production: false,
  apiUrl: 'http://localhost:8080',
  auth: {
    tokenStorageKey: 'jwt_token',
    endpoints: {
      login: '/auth/login',
      register: '/auth/register',
      me: '/users/me',
    },
  },
};
