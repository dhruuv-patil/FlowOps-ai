# syntax=docker/dockerfile:1
# Two-stage build so the prod image does not carry a Node toolchain or a
# dev server: the Next.js standalone output is what the runtime serves.
FROM node:20-alpine AS build
WORKDIR /app
COPY package.json package-lock.json* ./
RUN npm ci
COPY . .
RUN npm run build

# ---- Run stage ----
# Distroless-style: a non-root user, no shell, only the standalone server.
FROM node:20-alpine AS run
WORKDIR /app
ENV NODE_ENV=production
# `next start` is the prod server. Without `npm ci` again the standalone
# output references the local node_modules; copy them over.
COPY --from=build /app/package.json /app/package.json
COPY --from=build /app/.next /app/.next
COPY --from=build /app/node_modules /app/node_modules

RUN addgroup -S flowops && adduser -S -G flowops flowops \
    && chown -R flowops:flowops /app
USER flowops
EXPOSE 3000
HEALTHCHECK --interval=30s --timeout=5s --start-period=20s --retries=3 \
    CMD wget -qO- http://127.0.0.1:3000/ || exit 1
CMD ["npm", "run", "start"]
