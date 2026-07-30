# Verbamind Frontend

React + Vite client for the Verbamind API.

## Setup

```
npm install
cp .env.example .env
```

Edit `.env` and set `VITE_API_URL` to your backend's base URL
(e.g. `http://localhost:8080/api` for local dev, or your Render
backend URL for production — always include the `/api` suffix).

```
npm run dev
```

## Build

```
npm run build
```

Outputs static files to `dist/` — deploy that folder to Vercel,
Netlify, Render static sites, or any static host. Set the same
`VITE_API_URL` env var in your hosting platform's dashboard before
building for production.

## Notes on secrets

- No API keys, passwords, or backend secrets live in this codebase.
  The only configurable value is the backend's public URL.
- Auth tokens are kept in memory + sessionStorage, never
  localStorage, and are cleared on logout or tab close.
- The Razorpay integration only ever receives a public key_id and
  an order id from the backend — signature verification happens
  server-side, as it should.
- Document downloads go through an authenticated request (not a bare
  link) so the bearer token isn't exposed in browser history or logs.
