# TODO: Specify jvm & os versions
FROM clojure:latest as base


FROM base as build

WORKDIR /app

# Install npm
RUN curl -fsSL https://deb.nodesource.com/setup_18.x | bash - && apt-get install -y nodejs

COPY package.json package-lock.json ./
RUN npm ci

COPY deps.edn .
RUN clojure -P

COPY . .
RUN clojure -M:shadow-cljs release app
# NOTE: Maybe only use for staging?
RUN clojure -M:shadow-cljs release cards


FROM base as runner

# Create a directory for the app to run from
WORKDIR /app

# Set Evironment variables
ENV PORT=8080

# Expose port to the local machine
EXPOSE 8080

# Copy the entire folder contents to the image
COPY --from=build /app/resources/public/js/compiled /app/resources/public/js/compiled
# NOTE: Maybe only use for staging?
COPY --from=build /app/resources/public/js/workspaces /app/resources/public/js/workspaces
COPY . .

# Run the container calling the main function
CMD [ "clojure", "-M", "-m", "darbylaw.core" ]
