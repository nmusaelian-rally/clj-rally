(ns clj-rally.core
  (:gen-class)
  (:require [clj-http.client :as client]
            [clojure.data.json :as json]
            [clojure.edn]
            [clojure.string :as str]
            [clj-time.core :as t]))

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

(def config (clojure.edn/read-string (slurp "resources/config.edn")))

(def rally (get-in config [:rally]))

(defn subscription-info []
  (let [ sub-endpoint "subscription?fetch=subscriptionID,workspaces" empty-payload ""
        result (make-request :get (get-in rally [:auth]) (get-in rally [:base-url]) sub-endpoint empty-payload (get-in rally [:log-level]))
        sub-info {:sub-id         (get-in (json/read-str result) ["Subscription" "SubscriptionID"])
                  :sub-uuid       (get-in (json/read-str result) ["Subscription" "_refObjectUUID"])
                  :workspaces-url (second (str/split (get-in (json/read-str result) ["Subscription" "Workspaces" "_ref"]) #"v2.0" ))}]
    sub-info))

(defn context []
  (def payload "")
  (def log-level "info")
  (def headers   (get-in rally [:auth]))
  (def base-url  (get-in rally [:base-url]))
  (def workspaces-resource (second (str/split (get-in subscription-info [:workspaces-url]) #"v2.0"))) ; /Subscription/123/Workspaces
  (def workspace-name (get-in rally [:workspace]))
  (def project-name   (get-in rally [:project]))
  (def workspace-query (format "?query=(Name = \"%s\")&fetch=ObjectID,Projects" workspace-name))
  (def workspace-endpoint (str workspaces-resource workspace-query))
  (def wrk-result (make-request :get headers base-url workspace-endpoint payload log-level))
  (def workspace-oid (get-in (json/read-str wrk-result) ["QueryResult" "Results" 0 "ObjectID"]))
  (def projects-url  (get-in (json/read-str wrk-result) ["QueryResult" "Results" 0 "Projects" "_ref"]))
  (def projects-resource (second (str/split projects-url #"v2.0"))) ; /Workspace/456/Projects
  (def project-query (format "?query=(Name = \"%s\")&fetch=ObjectID" project-name))
  (def project-endpoint (str projects-resource project-query))
  (def proj-result (make-request :get headers base-url project-endpoint payload log-level))
  (def project-oid (get-in (json/read-str wrk-result) ["QueryResult" "Results" 0 "ObjectID"]))
  {:workspace workspace-oid :project project-oid}
  )


(def default-options
  (memoize
    (fn []
      {:context (context)})
    ))

(defn extended-options [& options]
  (let [options (merge default-options options)]
    options))

;(defn custom-header
;  []
;  (def sub (subscription-info))
;  (assoc (get-in rally [:auth]) :subscription sub)
;  )
;
;(defn -main [& args]
;  ;(def rally (read-config config))
;  (def headers   (get-in rally [:auth]))
;  (def base-url  (get-in rally [:base-url]))
;  (def workspace (get-in rally [:workspace]))
;  (def project   (get-in rally [:project]))
;  (def log-level (get-in rally [:log-level]))
;  (def empty-payload "")
;  (def workspaces-url (get-in (json/read-str result) ["Subscription" "Workspaces" "_ref"]))
;  (def sub (str sub-id " " sub-uuid))
;  (def context-oids (context))
;  (def create-endpoint (format "/story/create?workspace=/workspace/%s&project=/project/%s" (get context-oids :workspace) (get context-oids :project)))
;
;  (def payload (format "{\"HierarchicalRequirement\":
;    {\"Name\":\"clojure story %s\"}}" (str (t/time-now))))
;
;  ;(println(make-request :post (assoc headers :subscription sub) base-url create-endpoint payload log-level))
;  (println(make-request :post (subscription-info) base-url create-endpoint payload log-level))
;  )