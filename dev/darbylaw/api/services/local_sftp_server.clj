(ns darbylaw.api.services.local-sftp-server
  (:require [darbylaw.config :as config]
            [mount.core :as mount])
  (:import (org.apache.sshd.server SshServer)
           (org.apache.sshd.sftp.server SftpSubsystemFactory)
           (org.apache.sshd.server.auth.password PasswordAuthenticator)
           (org.apache.sshd.server.keyprovider SimpleGeneratorHostKeyProvider)
           (org.apache.sshd.common.file.virtualfs VirtualFileSystemFactory)
           (java.nio.file Files Paths)
           (java.nio.file.attribute FileAttribute)
           (java.net InetSocketAddress)
           (org.apache.sshd.server.auth.pubkey AcceptAllPublickeyAuthenticator)
           (org.apache.sshd.server.shell ProcessShellCommandFactory)
           (org.apache.sshd.scp.server ScpCommandFactory$Builder)))

(mount/defstate ^SshServer ssh-server
  :start (let [{:keys [enabled? port]} (-> config/config :post-service :local-sftp-server)]
           (when enabled?
             (let [dir (Paths/get ".post-sftp" (into-array String nil))]
               (Files/createDirectories dir (into-array FileAttribute nil))
               (doto (SshServer/setUpDefaultServer)
                 (.setPort port)
                 (.setKeyPairProvider
                   (doto (SimpleGeneratorHostKeyProvider.)
                     ; For JSch interop. See https://stackoverflow.com/questions/51969791/jschexception-verify-false-during-local-testing-with-apache-mina-sshd-server
                     (.setAlgorithm "RSA")))
                 (.setPublickeyAuthenticator AcceptAllPublickeyAuthenticator/INSTANCE)
                 (.setPasswordAuthenticator
                   (let [any-local-access (reify PasswordAuthenticator
                                            (authenticate [_this _username _password session]
                                              (-> ^InetSocketAddress (.getClientAddress session)
                                                .getAddress .isLoopbackAddress)))]
                     any-local-access))
                 (.setFileSystemFactory (VirtualFileSystemFactory. (.toAbsolutePath dir)))
                 (.setSubsystemFactories [(SftpSubsystemFactory.)])
                 (.setCommandFactory
                   (-> (ScpCommandFactory$Builder.)
                     (.withDelegate (ProcessShellCommandFactory.))
                     (.build)))
                 (.start)))))
  :stop (when ssh-server
          (.stop ssh-server)))

(comment
  (mount/stop #'ssh-server)
  (mount/start #'ssh-server))