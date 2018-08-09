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

