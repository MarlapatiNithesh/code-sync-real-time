# syntax=docker/dockerfile:1

# --- Stage 1: Build the Vite App ---
FROM node:18-alpine AS builder

WORKDIR /app

# Install dependencies
COPY package*.json ./
RUN npm ci

# Copy source and build
COPY . .
RUN npm run build

# --- Stage 2: Serve the built files using a lightweight server (like nginx or a node server) ---
# Option A: Using a simple static server (serve package)
FROM node:18-alpine AS runner

WORKDIR /app

# Install a lightweight static file server
RUN npm install -g serve

# Copy the built frontend
COPY --from=builder /app/dist ./dist

# Expose the port serve will run on
EXPOSE 5173

# Start the static server
CMD ["serve", "-s", "dist", "-l", "5173"]
