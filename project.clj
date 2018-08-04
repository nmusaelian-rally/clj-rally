
(defproject clj-rally "0.1.0-SNAPSHOT"
  :description "talk to rally"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [clj-http "3.9.0"]
                 [org.clojure/data.json "0.2.6"]
                 [clj-time "0.14.0"]]
  :main ^:skip-aot clj-rally.core
  :profiles {:uberjar {:aot :all}})