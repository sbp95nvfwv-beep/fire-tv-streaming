FROM node:22-alpine
WORKDIR /app
COPY server/package*.json ./
RUN npm ci --omit=dev
COPY server/src ./src
COPY web ./web
EXPOSE 8080
CMD ["npm", "start"]
