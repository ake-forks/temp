# NOTE:
# temurin-*     -> The jvm version
# -tools-deps-* -> The clojure version
# -bullseye     -> The debian version
FROM clojure:temurin-17-tools-deps-1.11.1.1165-bullseye as base

# Setup the directory for source code
WORKDIR /app

# Build & runtime deps
RUN apt-get update && apt-get install -y git libreoffice



FROM base as build

# Install clojure dependencies
# TODO: Only install build-time dependencies?
COPY deps.edn .
RUN clojure -A:dev -P

# Install npm
RUN clj -e '(-> "https://deb.nodesource.com/setup_18.x" slurp print)' | bash - && apt-get install -y nodejs

# Install node dependencies
COPY package.json package-lock.json ./
RUN npm ci

# Build & release
COPY . .
RUN npx shadow-cljs clj-run build/staging



FROM base as runner

# Install clojure dependencies
COPY deps.edn .
RUN clojure -P

# Setup the webapp's PORT
ENV PORT=8080
EXPOSE 8080

# Copy the entire folder contents to the image
COPY --from=build /app/resources/public/js/compiled /app/resources/public/js/compiled
# NOTE: Maybe only use for staging?
COPY --from=build /app/resources/public/js/workspaces /app/resources/public/js/workspaces
COPY . .

# Run the container calling the main function
CMD [ "clojure", "-M", "-m", "darbylaw.core" ]
