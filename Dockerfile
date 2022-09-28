FROM node:18-bullseye as build

# Install clojure & java
RUN apt-get update && apt-get -q -y install \
      openjdk-11-jdk \
      curl \
    && curl -s https://download.clojure.org/install/linux-install-1.11.1.1165.sh | bash

WORKDIR /app

COPY deps.edn .
RUN clojure -P

COPY package.json package-lock.json ./
RUN npm ci

COPY . .
RUN npm run release


# Fetch clojure image as the base
FROM clojure:latest as runner

# Create a directory for the app to run from
WORKDIR /app

# Copy the entire folder contents to the image
COPY --from=build /app/resources/public/js/compiled /app/resources/public/js/compiled
COPY . .

# Set Evironment variables
ENV PORT=8080

# Expose port to the local machine
EXPOSE 8080

# Run the container calling the main function
CMD [ "clojure", "-M", "-m", "darbylaw.core" ]
