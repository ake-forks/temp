(ns darbylaw.api.services.mailing
  (:require [clj-ssh.ssh :as ssh]
            [mount.core :as mount]
            [darbylaw.config :as config]
            [clojure.tools.logging :as log])
  (:import (org.apache.sshd.client SshClient)
           (org.apache.sshd.sftp.client.impl DefaultSftpClientFactory SimpleSftpClientImpl)
           (org.apache.sshd.client.keyverifier AcceptAllServerKeyVerifier)
           (java.nio.file Files Paths LinkOption)
           (java.nio.file.attribute FileAttribute)))

(defn create-known-hosts-file []
  (let [dir-path (Paths/get
                   (System/getProperty "user.home")
                   (into-array String [".ssh"]))
        path (.resolve dir-path "known_hosts")]
    (when-not (Files/exists path (into-array LinkOption []))
      (log/info "Creating file " (str path))
      (Files/createDirectories dir-path (into-array FileAttribute []))                      ;
      (Files/createFile path (into-array FileAttribute [])))))

(def ssh-agent
  (do
    (create-known-hosts-file)
    (ssh/ssh-agent {:use-system-ssh-agent false})))

(defn create-session [real|fake]
  (let [{:keys [host username password port] :or {port 22}}
        (-> config/config :mailing-service :ssh real|fake)]
    (doto (ssh/session ssh-agent host
            {:strict-host-key-checking :no
             :username username
             :password password
             :port port})
      (ssh/connect))))

; Prevents creation of multiple sessions by concurrent client threads.
(def connection-lock (Object.))

(mount/defstate ssh-session-atom
  :start (atom {:real nil
                :fake nil})
  :stop (locking connection-lock
          (doseq [real|fake [:real :fake]]
            (let [session (@ssh-session-atom real|fake)]
              (when session
                (ssh/disconnect session))))))

(defn obtain-ssh-session [real|fake]
  (locking connection-lock
    (let [session (@ssh-session-atom real|fake)]
      (if (or (nil? session)
              (not (ssh/connected? session)))
        (do
          ; Making sure the previous session disposes of resources
          ; See https://stackoverflow.com/a/72943084
          (when (some? session)
            (ssh/disconnect session))
          ; Sometimes it is not enough to reconnect an existing
          ; session object; creating a new session object is needed.
          ; See https://stackoverflow.com/a/30855201
          (let [new-session (create-session real|fake)]
            (swap! ssh-session-atom assoc real|fake new-session)
            (log/info "New SSH session created for account" real|fake)
            (when-not (ssh/connected? new-session)
              (log/error "Newly created session is not connected! Account:" real|fake))
            new-session))
        session))))

;; Public functions

(defn available? [real|fake]
  (try
    (let [session (obtain-ssh-session real|fake)]
      (ssh/sftp session {} :pwd))
    true
    (catch Exception exc
      (log/warn exc "SSH connection check failed!")
      false)))

(defn post-letter [real|fake from-path to-path]
  (let [session (obtain-ssh-session real|fake)]
    (ssh/sftp session {}
      :put from-path to-path)))

(defn run-sftp-command [real|fake cmd & args]
  (let [session (obtain-ssh-session real|fake)]
    (apply ssh/sftp session {} cmd args)))

(comment
  (mount/stop #'ssh-session-atom)
  (mount/start #'ssh-session-atom)

  (obtain-ssh-session :fake)
  (available? :fake)

  (run-sftp-command :fake :ls)
  (post-letter :fake "README.md" "README.md")
  (post-letter :real "README.md" "README.md")

  (ssh/connected? (@ssh-session-atom :fake))
  (ssh/connect (@ssh-session-atom :fake))
  (ssh/disconnect (@ssh-session-atom :fake))

  (mount/stop #'config/config)
  (mount/start #'config/config)

  (mount/stop #'darbylaw.api.services.local-sftp-server/ssh-server)
  (mount/start #'darbylaw.api.services.local-sftp-server/ssh-server))


; At some point we may want to move away from clj-ssh.
; Apache SSHD SshClient is a lower-level, but consistent alternative:
(comment
  ; Just use SimpleSftpClient
  (def sftp-client2
    (let [{:keys [host username password port]}
          (-> config/config :mailing-service :ssh)]
      (-> (SimpleSftpClientImpl. (SshClient/setUpDefaultSimpleClient))
        (.sftpLogin host port username password))))

  ; pwd
  (.canonicalPath sftp-client2 "")

  ; More elaborate initialization:
  (def ssh-client (doto (SshClient/setUpDefaultClient)
                    (.setServerKeyVerifier AcceptAllServerKeyVerifier/INSTANCE)
                    (.start)))

  (def ssh-session-atom (let [{:keys [host username password port]}
                              (-> config/config :mailing-service :ssh)]
                          (-> ssh-client
                            (.connect username host port)
                            (.verify 5000)
                            (.getSession))))

  (let [{:keys [password]}
        (-> config/config :mailing-service :ssh)]
    (.addPasswordIdentity ssh-session-atom password))
  (-> ssh-session-atom (.auth) (.verify 5000))

  (def sftp-client (-> DefaultSftpClientFactory/INSTANCE
                     (.createSftpClient ssh-session-atom))))
