# Only used if self-hosting instead of Vercel/Netlify. Vercel/Netlify build the
# repo directly and don't need this file — it exists for completeness/portability.

FROM node:22-alpine AS build
WORKDIR /app
COPY package*.json ./
RUN npm ci
COPY . .
RUN npm run build

FROM nginx:1.27-alpine
COPY --from=build /app/dist /usr/share/nginx/html
EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]
