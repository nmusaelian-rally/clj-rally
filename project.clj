
(defproject clj-rally "0.1.0-SNAPSHOT"
  :description "talk to rally"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}

  :repositories [["rally" "http://repo-depot.f4tech.com/artifactory/rally-maven"]
                 ["releases" {:url           "http://repo-depot.f4tech.com/artifactory/maven-local"
                              :sign-releases false
                              :username      ""
                              :password      ""}]
                 ["artifacts" {:url           "http://repo-depot.f4tech.com/artifactory/maven-local"
                               :sign-releases false
                               :username      ""
                               :password      ""}]]

  :dependencies [[org.clojure/clojure "1.8.0"]
                 ;[clj-http "3.9.1"]
                 [com.rallydev/cletus "1.1.171"]
                 [org.clojure/data.json "0.2.6"]
                 [clj-time "0.14.0"]
                 [org.clojure/tools.logging "0.4.1"]
                 [log4j/log4j "1.2.17" :exclusions [javax.mail/mail
                                                    javax.jms/jms
                                                    com.sun.jmdk/jmxtools
                                                    com.sun.jmx/jmxri]]]
  :main ^:skip-aot clj-rally.core
  :profiles {:uberjar {:aot :all}})