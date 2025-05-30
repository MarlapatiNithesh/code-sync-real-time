# syntax=docker/dockerfile:1

# --- Stage 1: Build the Vite App ---
FROM node:18-alpine AS builder

# Set working directory
WORKDIR /app

# Copy package files and install dependencies
COPY package*.json ./
RUN npm ci

# Copy source code and environment files
COPY . .

# Optional: if your env file is named differently, rename it here
# COPY .env.production .env

# Build the Vite app (uses .env if present)
RUN npm run build

# --- Stage 2: Serve the built files using serve ---
FROM node:18-alpine AS runner

WORKDIR /app

# Install serve
RUN npm install -g serve

# Copy only the built frontend files
COPY --from=builder /app/dist ./dist

# Expose the port the app will run on
EXPOSE 5173

# Serve the app
CMD ["serve", "-s", "dist", "-l", "5173"]
