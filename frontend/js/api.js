const API_BASE = 'http://localhost:8080/api';

const api = {
  _token: () => localStorage.getItem('riq_token'),

  _headers(isForm = false) {
    const h = { 'Authorization': `Bearer ${this._token()}` };
    if (!isForm) h['Content-Type'] = 'application/json';
    return h;
  },

  async _fetch(path, opts = {}) {
    const res = await fetch(API_BASE + path, opts);
    if (res.status === 401) { Auth.logout(); return null; }
    const ct = res.headers.get('content-type') || '';
    if (ct.includes('application/json')) return res.json();
    return res.text();
  },

  get(path) {
    return this._fetch(path, { headers: this._headers() });
  },

  post(path, data) {
    return this._fetch(path, {
      method: 'POST', headers: this._headers(),
      body: JSON.stringify(data)
    });
  },

  put(path, data) {
    return this._fetch(path, {
      method: 'PUT', headers: this._headers(),
      body: JSON.stringify(data)
    });
  },

  delete(path) {
    return this._fetch(path, { method: 'DELETE', headers: this._headers() });
  },

  upload(path, formData) {
    return this._fetch(path, {
      method: 'POST',
      headers: { 'Authorization': `Bearer ${this._token()}` },
      body: formData
    });
  },

  async downloadCsv() {
    const res = await fetch(API_BASE + '/analytics/export/csv', {
      headers: this._headers()
    });
    const blob = await res.blob();
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url; a.download = 'resumeiq_report.csv';
    a.click(); URL.revokeObjectURL(url);
  }
};
