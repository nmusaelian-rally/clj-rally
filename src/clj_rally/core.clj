(ns clj-rally.core
  (:gen-class)
  (:require [cletus.http-client :as client]
            ;[clj-http.client :as client]
            [clojure.data.json :as json]
            [clojure.edn]
            [clojure.string :as str]
            [clj-time.core :as t]
            [clojure.tools.logging :as log]
            [slingshot.slingshot :as slingshot :only [throw+ try+]]))

(declare rally)

(defn connection
  "Takes a config file and supplies bindings to forward-declared rally object."
  [config]
  (get-in config [:rally]))

(defn- make-request
  "Function for making CRUD http requests"
  [method headers endpoint & {:keys [payload query]}]
  (log/info (format "making request: %s" endpoint))
  (slingshot/try+
    (defn debug? []
      (if (= (get-in rally [:log-level]) "debug") true false))

    (:body (client/request {
                            :headers headers
                            :method method
                            :url (str (get-in rally [:base-url]) endpoint query)
                            :content-type "application/json"
                            :body payload
                            :debug (debug?)
                            :debug-body (debug?)}))

    (catch [:status 401] {:keys [body]}
      (log/error (str "Oh noes! Bad creds 401! Body:\n" body)))
    (catch [:status 404] {:keys [body]}
      (log/error (str "Oh noes! Page not found 404! Body:\n" body)))
    (catch Exception e (log/error (str "Oh noes! Exception:\n ") (.toString e)))
    (catch [:status 405] {:keys [body]}
      (log/error (str "Oh noes! Method not allowed 401! Body:\n" body)))
    ))

(defn subscription-info
  "Makes a GET request to return a hashmap of sub-id, sub-uuid and workspaces-url"
  []
  (let [ sub-endpoint "subscription?fetch=subscriptionID,workspaces"
        result (make-request :get (get-in rally [:auth]) sub-endpoint)
        sub-info {:sub-id         (get-in (json/read-str result) ["Subscription" "SubscriptionID"])
                  :sub-uuid       (get-in (json/read-str result) ["Subscription" "_refObjectUUID"])
                  :workspaces-url (second (str/split (get-in (json/read-str result) ["Subscription" "Workspaces" "_ref"]) #"v2.0" ))}]
    sub-info))

(defn context
  "Makes a GET request to return a hashmap of OIDs for workspace and project specified in config."
  []
  (let [headers (get-in rally [:auth])
        wrk-resource (get-in (subscription-info) [:workspaces-url])
        wrk-query    (format "&query=(Name = \"%s\")" (get-in rally [:workspace]))
        wrk-fetch    (format "?fetch=ObjectID,Projects")
        wrk-endpoint (str wrk-resource wrk-fetch)
        wrk-result   (make-request :get headers wrk-endpoint :query wrk-query )
        wrk-oid      (get-in (json/read-str wrk-result) ["QueryResult" "Results" 0 "ObjectID"])
        projects-url (get-in (json/read-str wrk-result) ["QueryResult" "Results" 0 "Projects" "_ref"])
        prj-resource (second (str/split projects-url #"v2.0"))
        prj-query    (format "&query=(Name = \"%s\")" (get-in rally [:project]))
        prj-fetch    (format "?fetch=ObjectID")
        prj-endpoint (str prj-resource prj-fetch)
        prj-result   (make-request :get headers prj-endpoint :query prj-query)
        prj-oid      (get-in (json/read-str prj-result) ["QueryResult" "Results" 0 "ObjectID"])]
    {:workspace wrk-oid :project prj-oid})
  )


(defn create-workitem
  "Takes two arguments: work item type and hashmap of the payload, e.g. {:Name name :PlanEstimate est}"
  [wi-type data]
  (let [record {(keyword wi-type) data}
        payload     (json/write-str record)
        context-oids (context)
        resource    "/%s/create?workspace=/workspace/%s&project=/project/%s"
        endpoint   (format resource wi-type (get context-oids :workspace) (get context-oids :project))
        result     (make-request :post (get-in rally [:auth]) endpoint :payload payload)]
    result))

(defn read-workitem
  "Takes three arguments: work item type, query string, e.g. (Name = \"DON'T DELETE\") and fetch string, e.g ObjectID,ScheduleState."
  [wi-type query-str fetch-str]
  (let [query (str "&query=" query-str)
        fetch (str "&fetch=" fetch-str)
        context-oids (context)
        resource "/%s?workspace=/workspace/%s&project=/project/%s"
        endpoint (format (str resource fetch) wi-type (get context-oids :workspace) (get context-oids :project))
        result (make-request :get (get-in rally [:auth]) endpoint :query query)]
    result)
  )

(defn update-workitem
  "Takes three arguments: work item type, hashmap of the payload, e.g {:ScheduleState target-state} and ObjectID of workitem"
  [wi-type data oid]
  (let [record {(keyword wi-type) data}
        payload     (json/write-str record)
        endpoint   (format "/%s/%s" wi-type oid)
        result     (make-request :post (get-in rally [:auth]) endpoint :payload payload)]
    result))


(defn delete-workitem
  "Takes two arguments: work item type and ObjectID of workitem"
  [wi-type oid]
  (let [endpoint   (format "/%s/%s" wi-type oid)
        result     (make-request :delete (get-in rally [:auth]) endpoint)]
    result))


(defn read-story
  []
  (let [wi-type    "HierarchicalRequirement"
        query      "(Name = \"DON'T DELETE\")"
        fetch      "ObjectID,ScheduleState"
        result     (read-workitem wi-type query fetch)
        name       (get-in (json/read-str result) ["QueryResult" "Results" 0 "_refObjectName"])]
    (println name)
    (println result)))

(defn -main [& args]
  (let [config (clojure.edn/read-string (slurp "resources/config.edn"))]
    (intern 'clj-rally.core 'rally (connection config)))

  (read-story)

  )