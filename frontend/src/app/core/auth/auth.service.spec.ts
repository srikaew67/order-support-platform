import { AuthService } from './auth.service';

describe('AuthService', () => {
  it('stores the access token after a successful session', () => {
    const service = new AuthService();
    service.storeSession('token-123');
    expect(localStorage.getItem('accessToken')).toBe('token-123');
  });
});
