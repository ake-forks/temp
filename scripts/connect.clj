#!/usr/bin/env bb

;; Connect to bastion server then open some ports locally that connect
;; to the RDS instances in the AWS account.

(require '[babashka.process :refer [sh shell]]
         '[cheshire.core :as json]
         '[clojure.string :as str])


;; >> Get Bastion Address
;; Just encode staticly? Just use an elastic IP?

(defn name-tag
  "Get's the value of the Tag who's Key is Name"
  [instance]
  (->> instance
       :Tags
       (some #(when (= "Name" (:Key %)) %))
       :Value))

(defn ->bastion-address
  [info]
  (str "ec2-user@" (:PublicIpAddress info)))

(def bastion
  (let [res (-> (sh "aws ec2 describe-instances")
                :out
                (json/parse-string true))
        instances (->> res
                       :Reservations
                       (map :Instances)
                       (apply concat))]
    (->> instances
         (some #(when (and (= "bastion" (name-tag %))
                           (= "running" (-> % :State :Name)))
                  %))
         ->bastion-address)))

(when (= "ec2-user@" bastion)
  (throw (Exception. "Could not find bastion server")))




;; >> Get DB Instances

(defn ->instance
  [info]
  (let [endpoint (:Endpoint info)]
    {:id (:DBInstanceIdentifier info)
     :host (:Address endpoint)
     :port (:Port endpoint)
     :db-name (:DBName info)
     :db-user (:MasterUsername info)}))

;; Only a guess at some ports that *should* be open
;; Aren't always available so may error
(def available-ports (range 10000 16000))

(def db-instances
  (let [res (-> (sh "aws rds describe-db-instances")
                :out
                (json/parse-string true))]
    (->> (:DBInstances res)
         (map ->instance)
         (map #(assoc %2 :local-port %1) available-ports))))

(when (empty? db-instances)
  (throw (Exception. "Could not find any db instances")))


;; >> SSH Command

(defn instance->local-forward
  "See `man ssh | less -p ' -L'`"
  [instance]
  ["-L" (str (:local-port instance)
             ":" (:host instance) ":" (:port instance))])

(defn ->ssh-command
  "Builds a command that connects io the bastion and sets up
  local port forwards to the given instances"
  [instances]
  (let [opts (->> instances
                  (map instance->local-forward)
                  (apply concat))]
    (concat ["ssh"] opts [bastion])))

(defn render-instances
  [instances]
  (str/join "\n\n"
    (for [instance instances]
      (str/join "\n"
        [(str "===")
         (str "Instance ID: " (:id instance))
         (str "Host: localhost")
         (str "Port: " (:local-port instance))
         (str "DB Name: " (:db-name instance))
         (str "DB User: " (:db-user instance))]))))



;; >> Print and connect to SSH
    
(let [instances db-instances
      ssh-command (->ssh-command instances)]
  (println (render-instances instances))
  (println "\n===")
  (println "Connecting to bastion (type 'exit' to exit)")
  (println ssh-command)
  (apply shell ssh-command))

;; So we don't print the output of `shell`
nil
