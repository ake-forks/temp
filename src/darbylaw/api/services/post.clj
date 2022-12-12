(ns darbylaw.api.services.post
  (:require [clj-ssh.ssh :as ssh]
            [mount.core :as mount]
            [darbylaw.config :as config]
            [clojure.tools.logging :as log])
  (:import (org.apache.sshd.client SshClient)
           (org.apache.sshd.sftp.client.impl DefaultSftpClientFactory SimpleSftpClientImpl)
           (org.apache.sshd.client.keyverifier AcceptAllServerKeyVerifier)))

(def ssh-agent
  (ssh/ssh-agent {:use-system-ssh-agent false}))

(defn create-session []
  (let [{:keys [host username password port]}
        (-> config/config :post-service :ssh)]
    (doto (ssh/session ssh-agent host
            {:strict-host-key-checking :no
             :username username
             :password password
             :port port})
      (ssh/connect))))

; Prevents creation of multiple sessions by concurrent client threads.
(def connection-lock (Object.))

(mount/defstate ssh-session-atom
  :start (atom nil)
  :stop (locking connection-lock
          (let [session @ssh-session-atom]
            (when session
              (ssh/disconnect session)))))

(defn obtain-ssh-session []
  (locking connection-lock
    (let [session @ssh-session-atom]
      (if (or (nil? session)
              (not (ssh/connected? session)))
        ; Sometimes it is not enough to reconnect an existing
        ; session object; creating a new session object is needed.
        ; See https://stackoverflow.com/a/30855201/503785
        (reset! ssh-session-atom (create-session))
        session))))

;; Public functions

(defn available? []
  (try
    (let [session (obtain-ssh-session)]
      (ssh/sftp session {} :pwd))
    true
    (catch Exception exc
      (log/warn exc "SSH connection check failed!")
      false)))

(defn post-letter [from-path to-path]
  (let [session (obtain-ssh-session)]
    (ssh/sftp session {}
      :put from-path to-path)))

(defn ls []
  (let [session (obtain-ssh-session)]
    (ssh/sftp session {} :ls)))

(comment
  (mount/stop #'ssh-session-atom)
  (mount/start #'ssh-session-atom)

  (obtain-ssh-session)
  (available?)
  (ls)
  (post-letter "README.md" "README.md")
  (post-letter "upload-test.txt" "upload-test.txt")

  (ssh/connected? @ssh-session-atom)
  (ssh/connect @ssh-session-atom)
  (ssh/disconnect @ssh-session-atom)

  (mount/stop #'config/config)
  (mount/start #'config/config))


; At some point we may want to move away from clj-ssh.
; Apache SSHD SshClient is a lower-level, but consistent alternative:
(comment
  ; Just use SimpleSftpClient
  (def sftp-client2
    (let [{:keys [host username password port]}
          (-> config/config :post-service :ssh)]
      (-> (SimpleSftpClientImpl. (SshClient/setUpDefaultSimpleClient))
        (.sftpLogin host port username password))))

  ; pwd
  (.canonicalPath sftp-client2 "")

  ; More elaborate initialization:
  (def ssh-client (doto (SshClient/setUpDefaultClient)
                    (.setServerKeyVerifier AcceptAllServerKeyVerifier/INSTANCE)
                    (.start)))

  (def ssh-session-atom (let [{:keys [host username password port]}
                              (-> config/config :post-service :ssh)]
                          (-> ssh-client
                            (.connect username host port)
                            (.verify 5000)
                            (.getSession))))

  (let [{:keys [password]}
        (-> config/config :post-service :ssh)]
    (.addPasswordIdentity ssh-session-atom password))
  (-> ssh-session-atom (.auth) (.verify 5000))

  (def sftp-client (-> DefaultSftpClientFactory/INSTANCE
                     (.createSftpClient ssh-session-atom))))
