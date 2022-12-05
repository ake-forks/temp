# darbylaw

## Clojure Server

### Prerequisites

#### Local AWS config

The server relies on [automatically inferring](https://docs.aws.amazon.com/sdk-for-java/latest/developer-guide/credentials.html#credentials-default) AWS credentials and configuration (region) from your environment.

Suggested setup: Using [aws-vault](https://github.com/99designs/aws-vault), which is an adapter for providing AWS credentials from your local password manager to AWS client programs:
- Make sure that you have set up access keys for your AWS user.
- Install [aws-vault](https://github.com/99designs/aws-vault) (Arch Linux: `pacman -S aws-vault`). 
- Add profile `aws-vault add juxtegg` (Replace "juxtegg" with your desired profile name; here I am using my AWS user name). You may need/want to specify your [password manager](https://github.com/99designs/aws-vault#vaulting-backends), for example: `aws-vault add juxtegg --backend pass`.
- Edit file `.aws/config` to contain:
    ```
    [profile juxtegg]
    credential_process = aws-vault exec juxtegg --json
    region=us-east-1
    ```
    Again, you may need the `--backend` option in the `credential_process` command line.
- When running the server, you will need to specify the AWS profile to use through:
  - An environment variable (`export AWS_PROFILE=juxtegg`)
  - Or just configure a default profile in `~/.aws/config` (section `[default]` with the same contents as above).

#### Local packages

For PDF generation, which uses [JODConverter](https://github.com/sbraconnier/jodconverter), you need to:
- Install LibreOffice. (In arch: `pacman -S libreoffice-still`).
- Make sure that `ps` command is accessible through your path. (This is usually the case. Otherwise, you may to install the `procps` package).

### Usage

Run a REPL with the `dev` and `test` alias. Then execute `(go!)` to start.

You can run the project's tests from that REPL, or from the command-line:

    $ clojure -M:test:runner

## ClojureScript Client

## Development

### Running the App

Run the server, as serves the client SPA.

Run:
```sh
npm install
npx shadow-cljs watch app
```

When `[:app] Build completed` appears in the output, browse to
[http://localhost:8080/](http://localhost:8080/).

You can connect to the nREPL exposed by shadow-cljs, and run `(shadow/repl :app)` to run a CLJS REPL against your running browser.

### Running workspaces cards

```sh
npm install
npx shadow-cljs watch cards
```

### Browser Setup

Browser caching should be disabled when developer tools are open to prevent interference with
[`shadow-cljs`](https://github.com/thheller/shadow-cljs) hot reloading.

Custom formatters must be enabled in the browser before
[CLJS DevTools](https://github.com/binaryage/cljs-devtools) can display ClojureScript data in the
console in a more readable way.

### Terraform

```sh
aws-vault exec juxtegg --no-session -- terraform apply -var "probatetree_docker_tag=b3ef7a1b5d5c40d13783c905405ed13c4db91e47"
```