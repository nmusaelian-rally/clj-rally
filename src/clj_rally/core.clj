(ns clj-rally.core
  (:require [clj-http.client :as client])
  (:require [clojure.data.json :as json])
  (:require [clojure.edn])
  (:require [clojure.string :as str])
  (:require [clj-time.core :as t]))

(defn make-request
  [method headers base-url endpoint payload log-level]
  (defn debug? []
    (if (= log-level "debug") true false))

  (:body
    (client/request
      {
       :headers headers
       :method method
       :url (str base-url endpoint)
       :content-type "application/json"
       :debug (debug?)
       :debug-body (debug?)
       :body payload})))


(defn context
  [workspaces-url headers base-url]
  (def endpoint (second (str/split workspaces-url #"v2.0")))
  (def workspace-endpoint (str endpoint "?query=(Name = \"NM LA\")&fetch=ObjectID,Projects"))
  (def wrk-result (make-request :get headers base-url workspace-endpoint "" "info"))
  (def workspace-oid (get-in (json/read-str wrk-result) ["QueryResult" "Results" 0 "ObjectID"]))
  (def projects-url  (get-in (json/read-str wrk-result) ["QueryResult" "Results" 0 "Projects" "_ref"]))
  (def endpoint2 (second (str/split projects-url #"v2.0")))
  (def project-endpoint (str endpoint2 "?query=(Name = BugzProject)&fetch=ObjectID"))
  (print project-endpoint)
  (def proj-result (make-request :get headers base-url project-endpoint "" "info"))
  (def project-oid (get-in (json/read-str wrk-result) ["QueryResult" "Results" 0 "ObjectID"]))
  {:workspace workspace-oid :project project-oid}
  )

(defn -main [& args]
  (def config (clojure.edn/read-string (slurp "resources/config.edn")))

  (def rally (:rally config))
  (def headers   (get-in rally [:auth]))
  (def base-url  (get-in rally [:base-url]))
  (def workspace (get-in rally [:workspace]))
  (def project   (get-in rally [:project]))

  (def empty-payload "")
  (def log-level "info")

  (def sub-endpoint "subscription?fetch=subscriptionID,workspaces")
  (def result   (make-request :get headers base-url sub-endpoint empty-payload log-level))
  (def sub-id   (get-in (json/read-str result) ["Subscription" "SubscriptionID"]))
  (def sub-uuid (get-in (json/read-str result) ["Subscription" "_refObjectUUID"]))
  (def workspaces-url (get-in (json/read-str result) ["Subscription" "Workspaces" "_ref"]))
  (def sub (str sub-id " " sub-uuid))
  (def context-oids (context workspaces-url headers base-url))
  (def create-endpoint (format "/story/create?workspace=/workspace/%s&project=/project/%s" (get context-oids :workspace) (get context-oids :project)))

  (def payload (format "{\"HierarchicalRequirement\":
    {\"Name\":\"clojure story %s\"}}" (str (t/time-now))))

  (println(make-request :post (assoc headers :subscription sub) base-url create-endpoint payload log-level))
  )
