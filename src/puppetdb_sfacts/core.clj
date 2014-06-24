(ns puppetdb-sfacts.core
  (:import (com.jolbox.bonecp BoneCPDataSource BoneCPConfig))
  (:require [clojure.java.jdbc :as jdbc]
            [migratus.core :as migratus]
            [schema.core :as s]
            [clojure.string :as string]))

;; SCHEMAS

(def FactPath [s/Str])

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
   "value_types"
   "fact_values"
   "facts"
   "factsets"])

;; TODO still need to work out what this should be, either way we need special
;; handling around it.
(def path-delimiter
  ":")

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

(defn migrate
  "Perform all migrations."
  []
  (try
    (migratus/migrate migrate-config)
    (catch java.sql.BatchUpdateException e
      (jdbc/print-sql-exception-chain e)
      (System/exit 2))))

;; DB HELPERS

(s/defn ^:always-validate drop-table
  "Drops a table from the database"
  [db
   table :- s/Str]
  (let [ddl (str "DROP TABLE " table " CASCADE")]
    (jdbc/execute! db [ddl])))

(s/defn ^:always-validate drop-tables
  "Drops a number of tables at once"
  [db
   tables :- [s/Str]]
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

;; RANDOM

(s/defn ^:always-validate random-string
  "Create random string"
  [length :- s/Int]
  (let [ascii-codes (concat (range 97 123))]
    (apply str (repeatedly length #(char (rand-nth ascii-codes))))))

(s/defn ^:always-validate random-strings
  "Create a series of random strings"
  [length :- s/Int
   amount :- s/Int]
  (loop [x amount
         data []]
    (if (> x 0)
      (recur (dec x) (conj data (random-string length)))
      data)))

;; STORAGE

(s/defn ^:always-validate insert-certname
  "Load up a certname"
  [db
   certname :- s/Str]
  (jdbc/insert! db :certnames {:certname certname}))

(s/defn ^:always-validate encode-fact-path :- s/Str
  "Encode fact-path before storage"
  [path :- FactPath]
  ;; TODO: escape handling for items that contain the delimiter
  (string/join path-delimiter path))

(s/defn ^:always-validate insert-fact-path
  "Load up a fact path"
  [db
   path :- FactPath
   type :- s/Str]
  (let [result  (jdbc/query db ["select id from value_types where type = ?" type])
        type-id (:id (first result))
        encoded-path (encode-fact-path path)]
    (jdbc/insert! db "fact_paths" {"value_type_id" type-id "path" encoded-path})))

;; RANDOM LOADERS

(s/defn ^:always-validate insert-certnames
  "Load up a series of random certnames"
  [db
   amount :- s/Int]
  (doseq [string  (random-strings 10 30)]
    (insert-certname db string)))

;; OTHER

(defn foo
  []
  (println "Hello, World!")
  (with-db db
    (jdbc/query db ["select count(*) from fact_values"])))

(s/defn ^:always-validate bar
  "bar"
  [foo :- s/Str]
  (println "bar"))
