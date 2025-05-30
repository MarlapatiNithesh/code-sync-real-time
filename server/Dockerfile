# syntax=docker/dockerfile:1

# --- Stage 1: Build the application ---
FROM node:18-alpine AS builder

WORKDIR /app

# Install dependencies
COPY package*.json ./
RUN npm ci

# Explicitly copy .env first (make sure it's in your build context)
COPY .env ./

# Copy the rest of your source files
COPY . ./

# Optional: list files to verify .env presence
RUN ls -la /app

# Build the app (update if you're using tsc, vite, next, etc.)
RUN npm run build

# Remove dev dependencies for a lighter production image
RUN npm prune --omit=dev

# --- Stage 2: Production image ---
FROM node:18-alpine AS runner

WORKDIR /app

# Copy build output and dependencies from builder
COPY --from=builder /app/dist ./dist
COPY --from=builder /app/public ./public
COPY --from=builder /app/node_modules ./node_modules
COPY --from=builder /app/package*.json ./

# Copy the .env file explicitly with full path
COPY --from=builder /app/.env /app/.env

# Set environment
ENV NODE_ENV=production

# Expose the application port
EXPOSE 3000

# Run the app
CMD ["npm", "run", "start"]
