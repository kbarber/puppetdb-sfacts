(defproject puppetdb-sfacts "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :pedantic? :abort
  :dependencies [[org.clojure/tools.logging "0.2.6"]
                 [ch.qos.logback/logback-classic "1.1.1"]
                 [org.clojure/clojure "1.6.0"]
                 [com.jolbox/bonecp "0.8.0.RELEASE" :exclusions [org.slf4j/slf4j-api]]
                 [org.clojure/java.jdbc "0.3.3"]
                 [org.postgresql/postgresql "9.3-1101-jdbc41"]
                 [migratus "0.7.0"]
                 [prismatic/schema "0.2.4"]]
  :plugins [[migratus-lein "0.1.0" :exclusions [org.clojure/clojure]]]
  :migratus {:store :database
             :migration-dir "migrations"
             :db {:classname "org.postgresql.Driver"
                  :subprotocol "postgresql"
                  :subname "//localhost:5432/sfacts"
                  :user "ken"
                  :password ""}})
