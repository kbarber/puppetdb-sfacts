(ns puppetdb-sfacts.core
  (:import (com.jolbox.bonecp BoneCPDataSource BoneCPConfig))
  (:require [clojure.java.jdbc :as jdbc]
            [migratus.core :as migratus]))

;; CONFIG

(def db-config
  {:classname "org.postgresql.Driver"
   :subprotocol "postgresql"
   :subname "//localhost:5432/sfacts"
   :user "ken"
   :password ""})

(def migrate-config
  {:store :database
   :migration-dir "migrations"
   :db db-config})

(def tables
  ["certnames"
   "fact_paths"
   "fact_types"
   "fact_values"
   "facts"
   "factsets"])

;; DB HELPERS

(def db-url
  (let [{:keys [subprotocol subname]} db-config]
    (str "jdbc:" subprotocol ":" subname)))

(def dbspec
  (let [{:keys [user password]} db-config
        config (doto (new BoneCPConfig)
                 (.setDefaultAutoCommit false)
                 (.setLazyInit true)
                 (.setJdbcUrl  db-url)
                 (.setUsername user)
                 (.setPassword password))]
    {:datasource (BoneCPDataSource. config)}))

(defn with-db-fn
  [f]
  (jdbc/with-db-transaction [t-con dbspec :isolation :serializable]
    (f t-con)))

(defmacro with-db
  "Run body with a db"
  [db-var & body]
  `(with-db-fn (fn [~db-var] (do ~@body))))

;; MIGRATION

(defn migrate []
  (try
    (migratus/migrate migrate-config)
    (catch java.sql.BatchUpdateException e
      (throw (.getNextException e)))))

;; DB HELPERS

(defn drop-table
  "Drops a table from the database"
  [db table]
  (let [ddl (str "DROP TABLE " table " CASCADE")]
    (jdbc/execute! db [ddl])))

(defn drop-tables
  "Drops a number of tables at once"
  [db tables]
  (doseq [table tables]
    (drop-table db table)))

(defn wipe-db
  "Remove all tables from the database"
  [db]
  (drop-tables db tables)
  (drop-table db "schema_migrations"))

(defn reload-db
  "Remove all tables and run migrations again."
  []
  (with-db db
    (wipe-db db))
  (migrate))

(defn foo []
  (println "Hello, World!")
  (with-db db
    (jdbc/query db ["select count(*) from fact_values"])))
