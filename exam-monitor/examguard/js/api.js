// ── Base URL — matches your controllers exactly
const API_BASE = 'http://localhost:8080/api';

// ── Auth helpers
const Auth = {
    setToken(token) { localStorage.setItem('jwt', token); },
    getToken()      { return localStorage.getItem('jwt'); },
    setEmail(email) { localStorage.setItem('pendingEmail', email); },
    getEmail()      { return localStorage.getItem('pendingEmail'); },
    setRole(role)   { localStorage.setItem('userRole', role); },
    getRole()       { return localStorage.getItem('userRole'); },
    isLoggedIn()    { return !!localStorage.getItem('jwt'); },
    isAdmin()       { return localStorage.getItem('userRole') === 'ROLE_ADMIN'; },
    logout() {
        localStorage.clear();
        window.location.href = 'index.html';
    },
    requireAuth() {
        if (!Auth.isLoggedIn()) window.location.href = 'index.html';
    },
    requireAdmin() {
        Auth.requireAuth();
        if (!Auth.isAdmin()) window.location.href = 'dashboard.html';
    }
};

// ── Core fetch wrapper
const Api = {
    async request(method, path, body = null, auth = true) {
        const headers = { 'Content-Type': 'application/json' };

        if (auth) {
            const token = Auth.getToken();
            if (!token) { window.location.href = 'index.html'; return; }
            headers['Authorization'] = `Bearer ${token}`;
        }

        const options = { method, headers };
        if (body) options.body = JSON.stringify(body);

        const res = await fetch(`${API_BASE}${path}`, options);

        let data;
        try { data = await res.json(); } catch { data = {}; }

        if (!res.ok) {
            const msg = typeof data === 'string' ? data : (data.message || 'Request failed');
            throw { status: res.status, message: msg };
        }
        return data;
    },

    // Auth (no JWT needed)
    register:     (body) => Api.request('POST',   '/auth/register',    body, false),
    login:        (body) => Api.request('POST',   '/auth/login',       body, false),
    verifyOtp:    (body) => Api.request('POST',   '/auth/verify-otp',  body, false),

    // Exams (JWT required)
    getExams:     ()     => Api.request('GET',    '/exams'),
    getExam:      (id)   => Api.request('GET',    `/exams/${id}`),
    submitExam:   (body) => Api.request('POST',   '/exams/submit',     body),
    createExam:   (body) => Api.request('POST',   '/exams',            body),
    deleteExam:   (id)   => Api.request('DELETE', `/exams/${id}`),

    // Results (JWT required)
    getMyResults: ()     => Api.request('GET',    '/exams/results/my'),
    getAllResults: ()     => Api.request('GET',    '/exams/results'),
};

// ── UI helpers
function showAlert(id, msg, type = 'error') {
    const el = document.getElementById(id);
    if (!el) return;
    el.className = `alert alert-${type} show`;
    el.textContent = msg;
}
function hideAlert(id) {
    const el = document.getElementById(id);
    if (el) { el.className = 'alert'; el.textContent = ''; }
}
function setLoading(btnId, loading, text = 'Submit') {
    const btn = document.getElementById(btnId);
    if (!btn) return;
    btn.disabled = loading;
    btn.textContent = loading ? 'Please wait…' : text;
}
function formatDate(str) {
    if (!str) return '—';
    const d = new Date(str);
    return isNaN(d) ? str : d.toLocaleDateString('en-IN', {
        day: '2-digit', month: 'short', year: 'numeric',
        hour: '2-digit', minute: '2-digit'
    });
}
function scoreColor(score, total) {
    const pct = total ? (score / total) * 100 : 0;
    if (pct >= 75) return 'var(--green-d)';
    if (pct >= 50) return 'var(--warn)';
    return 'var(--danger)';
}
function showToast(msg, type = 'default', duration = 3000) {
    let c = document.getElementById('toast-container');
    if (!c) {
        c = document.createElement('div');
        c.id = 'toast-container';
        c.className = 'toast-container';
        document.body.appendChild(c);
    }
    const t = document.createElement('div');
    t.className = `toast ${type}`;
    t.textContent = msg;
    c.appendChild(t);
    setTimeout(() => {
        t.style.opacity = '0';
        t.style.transition = '0.3s';
        setTimeout(() => t.remove(), 300);
    }, duration);
}