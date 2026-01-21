FROM node:20-alpine AS builder

WORKDIR /app

ARG VITE_API_URL=/api
ENV VITE_API_URL=$VITE_API_URL

RUN npm install -g bun

COPY package.json bun.lock ./

RUN bun install --frozen-lockfile

COPY . .

RUN bun run build

FROM alpine:latest

WORKDIR /static

COPY --from=builder /app/dist .

CMD ["echo", "Static files ready at /static"]
