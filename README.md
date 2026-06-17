# Cricbook Workspace

This repository is organized into three app surfaces:

- `backend/` - Spring Boot API and backend assets
- `frontend/` - Next.js web app
- `mobile/` - Expo React Native app

Common commands:

- Backend: `cd backend && .\\mvnw.cmd spring-boot:run`
- Frontend: `cd frontend && npm run dev`
- Mobile: `cd mobile && npm start`

Notes:

- The backend source was moved from the repository root into `backend/`.
- `payload.json` is still at the root because it was not part of the app structure move.
