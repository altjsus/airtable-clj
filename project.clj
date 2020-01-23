(defproject airtable-clj "0.1.0-SNAPSHOT"
  :description "A Clojure client for the Airtable API."
  :url "https://github.com/EvanHahn/airtable-clj"
  :license {:name "MIT License"
            :url "https://opensource.org/licenses/MIT"}
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [clj-http "3.8.0"]
                 [cheshire "5.8.0"]
                 [environ "1.1.0"]]
  :main airtable-clj.core
  :profiles {:dev {:dependencies [[com.squareup.okhttp3/mockwebserver "3.10.0"]]
                   :plugins [[lein-cljfmt "0.5.7"]]}})
