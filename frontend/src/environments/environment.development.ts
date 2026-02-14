export const environment = {
  production: false,
  apiUrl: 'https://localhost:8443',
  auth: {
    tokenStorageKey: 'jwt_token',
    endpoints: {
      login: '/auth/login',
      register: '/auth/register',
      me: '/users/me',
    },
  },
};
