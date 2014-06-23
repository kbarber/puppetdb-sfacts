(ns puppetdb-sfacts.core
  (:import (com.jolbox.bonecp BoneCPDataSource BoneCPConfig))
  (:require [clojure.java.jdbc :as jdbc]
            [migratus.core :as migratus]))

(defn dbspec []
  (let [config (doto (new BoneCPConfig)
                 (.setDefaultAutoCommit false)
                 (.setLazyInit true)
                 (.setJdbcUrl "jdbc:postgresql://localhost:5432/sfacts")
                 (.setUsername "ken")
                 (.setPassword ""))]
    {:datasource (BoneCPDataSource. config)}))

(defn migrate []
  (let [config {:store :database
                :migration-dir "migrations"
                :db {:classname "org.postgresql.Driver"
                     :subprotocol "postgresql"
                     :subname "//localhost:5432/sfacts"
                     :user "ken"
                     :password ""}}]
    (try
      (migratus/migrate config)
      (catch java.sql.BatchUpdateException e
        (throw (.getNextException e))))))

(defn foo []
  (println "Hello, World!")
  (let [db-spec (dbspec)
        db (jdbc/get-connection db-spec)]
    (jdbc/with-db-transaction [t-con db-spec :isolation :serializable]
      (jdbc/query t-con ["select count(*) from fact_values"]))))
