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

# Declare build arguments for Vite compilation
ARG VITE_BACKEND_URL
ARG VITE_API_URL
ENV VITE_BACKEND_URL=$VITE_BACKEND_URL
ENV VITE_API_URL=$VITE_API_URL

# Build the Vite app
RUN npm run build

# --- Stage 2: Serve using Nginx ---
FROM nginx:alpine AS runner

# Copy only the built frontend files to Nginx public directory
COPY --from=builder /app/dist /usr/share/nginx/html

# Copy the Nginx configuration
COPY nginx.conf /etc/nginx/conf.d/default.conf

# Expose standard HTTP port
EXPOSE 80

# Serve the app
CMD ["nginx", "-g", "daemon off;"]
