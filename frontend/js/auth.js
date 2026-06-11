const Auth = {
  login(data) {
    localStorage.setItem('riq_token', data.token);
    localStorage.setItem('riq_user', JSON.stringify(data));
  },
  logout() {
    localStorage.removeItem('riq_token');
    localStorage.removeItem('riq_user');
    Router.go('login');
  },
  user() {
    try { return JSON.parse(localStorage.getItem('riq_user')); } catch { return null; }
  },
  isLoggedIn() { return !!localStorage.getItem('riq_token'); }
};
