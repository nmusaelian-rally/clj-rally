(ns clj-rally.core
  (:gen-class)
  (:require [clj-http.client :as client]
            [clojure.data.json :as json]
            [clojure.edn]
            [clojure.string :as str]
            [clj-time.core :as t]))

(def config (clojure.edn/read-string (slurp "resources/config.edn")))

(def rally (get-in config [:rally]))

(defn make-request
  [method headers endpoint payload]
  (defn debug? []
    (if (= (get-in rally [:log-level]) "debug") true false))

  (:body
    (client/request
      {
       :headers headers
       :method method
       :url (str (get-in rally [:base-url]) endpoint)
       :content-type "application/json"
       :debug (debug?)
       :debug-body (debug?)
       :body payload})))

(defn subscription-info []
  (let [ sub-endpoint "subscription?fetch=subscriptionID,workspaces" empty-payload ""
        result (make-request :get (get-in rally [:auth]) sub-endpoint empty-payload)
        sub-info {:sub-id         (get-in (json/read-str result) ["Subscription" "SubscriptionID"])
                  :sub-uuid       (get-in (json/read-str result) ["Subscription" "_refObjectUUID"])
                  :workspaces-url (second (str/split (get-in (json/read-str result) ["Subscription" "Workspaces" "_ref"]) #"v2.0" ))}]
    sub-info))

(defn context []
  (let [payload ""
        headers (get-in rally [:auth])
        workspaces-resource (get-in (subscription-info) [:workspaces-url])
        workspace-query (format "?query=(Name = \"%s\")&fetch=ObjectID,Projects" (get-in rally [:workspace]))
        workspace-endpoint (str workspaces-resource workspace-query)
        wrk-result (make-request :get headers workspace-endpoint payload)
        workspace-oid (get-in (json/read-str wrk-result) ["QueryResult" "Results" 0 "ObjectID"])
        projects-url  (get-in (json/read-str wrk-result) ["QueryResult" "Results" 0 "Projects" "_ref"])
        projects-resource (second (str/split projects-url #"v2.0"))
        project-query (format "?query=(Name = \"%s\")&fetch=ObjectID" (get-in rally [:project]))
        project-endpoint (str projects-resource project-query)
        proj-result (make-request :get headers project-endpoint payload)
        project-oid (get-in (json/read-str proj-result) ["QueryResult" "Results" 0 "ObjectID"])]
    {:workspace workspace-oid :project project-oid})
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