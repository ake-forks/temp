(ns darbylaw.api.services.post
  (:require [clj-ssh.ssh :as ssh]
            [mount.core :as mount]
            [darbylaw.config :as config]
            [clojure.tools.logging :as log])
  (:import (org.apache.sshd.client SshClient)
           (org.apache.sshd.sftp.client.impl DefaultSftpClientFactory SimpleSftpClientImpl)
           (org.apache.sshd.client.keyverifier AcceptAllServerKeyVerifier)))

(mount/defstate ssh-agent
  :start (ssh/ssh-agent {:use-system-ssh-agent false}))

(mount/defstate ssh-session
  :start (let [{:keys [host username password port]}
               (-> config/config :post-service :ssh)]
           (ssh/session ssh-agent host
             {:strict-host-key-checking :no
              :username username
              :password password
              :port port}))
  :stop (ssh/disconnect ssh-session))

; clj-ssh operations contain their own check, but they will actually fail
; if session is not connected when creating a channel.
(defn ensure-connected! [session]
  (when-not (ssh/connected? session)
    (ssh/connect session)))

(defn post-letter [from-path to-path]
  (ensure-connected! ssh-session)
  (ssh/sftp ssh-session {}
    :put from-path to-path))

(defn ls []
  (ensure-connected! ssh-session)
  (ssh/sftp ssh-session {} :ls))

(defn available? []
  (try
    (ensure-connected! ssh-session)
    (ssh/sftp ssh-session {} :pwd)
    true
    (catch Exception exc
      (log/warn exc "SSH connection check failed!")
      false)))

(comment
  (mount/stop #'ssh-session)
  (mount/start #'ssh-session)

  (ensure-connected! ssh-session)
  (available?)
  (ls)
  (post-letter "README.md" "README.md")
  (post-letter "upload-test.txt" "upload-test.txt")

  (ssh/connected? ssh-session)
  (ssh/connect ssh-session)
  (ssh/disconnect ssh-session)

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

  (def ssh-session (let [{:keys [host username password port]}
                         (-> config/config :post-service :ssh)]
                     (-> ssh-client
                       (.connect username host port)
                       (.verify 5000)
                       (.getSession))))

  (let [{:keys [password]}
        (-> config/config :post-service :ssh)]
    (.addPasswordIdentity ssh-session password))
  (-> ssh-session (.auth) (.verify 5000))

  (def sftp-client (-> DefaultSftpClientFactory/INSTANCE
                     (.createSftpClient ssh-session))))
