# ExamGuard Frontend

A clean, modern frontend for the ExamGuard exam monitoring system.

## Folder Structure

```
examguard/
├── index.html          ← Landing page (choose User / Admin)
├── register.html       ← Student registration
├── user-login.html     ← Student login
├── admin-login.html    ← Admin login
├── otp.html            ← OTP verification (shared)
├── dashboard.html      ← Student exam list
├── exam.html           ← Exam attempt page
├── result.html         ← Student results
├── admin.html          ← Admin panel
├── css/
│   └── style.css       ← All styles
└── js/
    └── api.js          ← API calls, Auth helpers, utilities
```

## Setup Instructions

### Step 1 — Configure Backend URL

Open `js/api.js` and update line 2:

```js
const API_BASE = 'http://localhost:8080/api';
// Change to your backend URL, e.g.:
// const API_BASE = 'https://your-backend.com/api';
```

### Step 2 — Enable CORS in Spring Boot

In your `SecurityConfig.java`, make sure CORS is enabled:

```java
@Bean
public CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration config = new CorsConfiguration();
    config.setAllowedOrigins(List.of("*")); // or your frontend URL
    config.setAllowedMethods(List.of("GET","POST","PUT","DELETE","OPTIONS"));
    config.setAllowedHeaders(List.of("*"));
    config.setExposedHeaders(List.of("Authorization"));
    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", config);
    return source;
}
```

And in your `SecurityFilterChain`:
```java
http.cors(cors -> cors.configurationSource(corsConfigurationSource()))
```

### Step 3 — Run the Frontend

**Option A: Open directly (simple)**
Just open `index.html` in your browser. Works for most browsers.

**Option B: Use VS Code Live Server (recommended)**
1. Install "Live Server" extension in VS Code
2. Right-click `index.html` → "Open with Live Server"

**Option C: Python HTTP server**
```bash
cd examguard
python -m http.server 3000
# Open http://localhost:3000
```

**Option D: Node.js**
```bash
npx serve examguard
```

## Authentication Flow

```
index.html
  ├── user-login.html  → otp.html → dashboard.html → exam.html → result.html
  └── admin-login.html → otp.html → admin.html
```

### JWT Storage
- JWT stored in `localStorage` as `jwt`
- Role stored as `userRole` (`ROLE_USER` or `ROLE_ADMIN`)
- All API calls auto-include `Authorization: Bearer <token>`

## API Response Expectations

### POST /auth/verify-otp
Expected response:
```json
{ "token": "eyJhbGc..." }
```
If your backend returns a different key (e.g. `jwt`), update `otp.html` line:
```js
const token = data.token || data.jwt || data;
```

### GET /exams
Expected response:
```json
[
  {
    "id": "exam123",
    "title": "Data Structures",
    "description": "...",
    "duration": 30,
    "questions": [
      {
        "id": "q1",
        "questionText": "What is a stack?",
        "options": ["LIFO", "FIFO", "Random", "None"],
        "correctAnswer": "LIFO"
      }
    ]
  }
]
```

### POST /exams/submit
Request body sent (email comes from JWT on backend):
```json
{
  "examId": "exam123",
  "answers": {
    "q1": "LIFO",
    "q2": "Binary Tree"
  }
}
```

### GET /exams/results/my
```json
[
  {
    "id": "...",
    "userEmail": "student@example.com",
    "examId": "exam123",
    "score": 8,
    "totalMarks": 10,
    "submittedAt": "2024-01-15T10:30:00"
  }
]
```

## Customization

| What | Where |
|------|-------|
| Backend URL | `js/api.js` line 2 |
| Colors / fonts | `css/style.css` `:root` variables |
| OTP length | `otp.html` — add/remove `.otp-box` inputs |
| Exam card icons | `dashboard.html` — `ICONS` array |
