# syntax=docker/dockerfile:1

# --- Stage 1: Build the application ---
FROM node:18-alpine AS builder

WORKDIR /app

# Install dependencies
COPY package*.json ./
RUN npm ci

# Copy source files
COPY . .

# Build the app (adjust if you’re using tsc, vite, next, etc.)
RUN npm run build

# Remove dev dependencies
RUN npm prune --omit=dev

# --- Stage 2: Production image ---
FROM node:18-alpine AS runner

WORKDIR /app

# Copy necessary files from builder
COPY --from=builder /app/dist ./dist
COPY --from=builder /app/public ./public
COPY --from=builder /app/node_modules ./node_modules
COPY --from=builder /app/package*.json ./

# Environment
ENV NODE_ENV=production

# Expose port
EXPOSE 3000

# Start the server (adjust this if needed)
CMD ["npm", "run", "start"]
