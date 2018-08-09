(ns clj-rally.core
  (:gen-class)
  (:require [clj-http.client :as client]
            [clojure.data.json :as json]
            [clojure.edn]
            [clojure.string :as str]
            [clj-time.core :as t]
            [clojure.tools.logging :as log]
            [slingshot.slingshot :as slingshot :only [throw+ try+]]))

(declare rally)

(defn connection
  [config]
  (get-in config [:rally]))

(defn make-request
  [method headers endpoint & {:keys [payload query-params]}]
  (log/info (format "making request: %s" endpoint))
  (slingshot/try+
    (defn debug? []
      (if (= (get-in rally [:log-level]) "debug") true false))

    (:body (client/request {
                            :headers headers
                            :method method
                            :url (str (get-in rally [:base-url]) endpoint)
                            :content-type "application/json"
                            :body payload
                            :query-params query-params
                            :debug (debug?)
                            :debug-body (debug?)}))

    (catch [:status 401] {:keys [body]}
      (log/error (str "Oh noes! Bad creds 401! Body:\n" body)))
    (catch [:status 404] {:keys [body]}
      (log/error (str "Oh noes! Page not found 404! Body:\n" body)))
    (catch Exception e (log/error (str "Oh noes! Exception:\n ") (.toString e)))
    ))

(defn subscription-info []
  (let [ sub-endpoint "subscription?fetch=subscriptionID,workspaces"
        result (make-request :get (get-in rally [:auth]) sub-endpoint)
        sub-info {:sub-id         (get-in (json/read-str result) ["Subscription" "SubscriptionID"])
                  :sub-uuid       (get-in (json/read-str result) ["Subscription" "_refObjectUUID"])
                  :workspaces-url (second (str/split (get-in (json/read-str result) ["Subscription" "Workspaces" "_ref"]) #"v2.0" ))}]
    sub-info))

(defn context []
  (let [headers (get-in rally [:auth])
        workspaces-resource (get-in (subscription-info) [:workspaces-url])
        workspace-query-params {"Name" (get-in rally [:workspace])}
        workspace-fetch (format "?fetch=ObjectID,Projects")
        workspace-endpoint (str workspaces-resource workspace-fetch)
        wrk-result (make-request :get headers workspace-endpoint :query-params workspace-query-params )
        workspace-oid (get-in (json/read-str wrk-result) ["QueryResult" "Results" 0 "ObjectID"])
        projects-url  (get-in (json/read-str wrk-result) ["QueryResult" "Results" 0 "Projects" "_ref"])
        projects-resource (second (str/split projects-url #"v2.0"))
        project-query-params {"Name" (get-in rally [:project])}
        project-fetch (format "?fetch=ObjectID")
        project-endpoint (str projects-resource project-fetch)
        proj-result (make-request :get headers project-endpoint :query-params project-query-params)
        project-oid (get-in (json/read-str proj-result) ["QueryResult" "Results" 0 "ObjectID"])]
    {:workspace workspace-oid :project project-oid})
  )

