(ns clj-rally.core-test
  (:require [clojure.test :refer :all]
            [clojure.string :as str]
            [clojure.data.json :as json]
            [clj-rally.core :refer :all]
            [clojure.tools.logging :as log]
            [clj-time.core :as t]))

(defn random-name
  []
  (let [adjec  (vector "fatuous", "optimistic", "officious", "unctuous")
        nouns  (vector "troglodyte", "fantomas", "pony", "puffery")
        rand1  (rand-int (count adjec))
        rand2   (rand-int (count nouns))
        name    (str/join " " [(get adjec rand1) (get nouns rand2) (str (t/time-now))])]
    name))

(deftest subscription-info-test
  (let [result (subscription-info)]
    (is (= (get-in result [:workspaces-url]) "/Subscription/1154643/Workspaces"))
    (is (= (get-in result [:sub-id]) 209))
    (is (= (get-in result [:sub-uuid]) "13eb4d62-3c4f-442e-b825-9f6786726d99"))))

(deftest context-test
  (let [result (context)]
    (is (= (get-in result [:workspace]) 1572380957))
    (is (= (get-in result [:project]) 1572381037))))

;(deftest create-n-delete-test
;  (let [artifact-type "HierarchicalRequirement"
;        payload (format "{\"%s\": {\"Name\":\"another clojure toolkit story %s\"}}" artifact-type (str (t/time-now)))
;        context-oids (context)
;        story-resource "/%s/create?workspace=/workspace/%s&project=/project/%s"
;        create-endpoint (format story-resource artifact-type (get context-oids :workspace) (get context-oids :project))]
;    (let [result (make-request :post (get-in rally [:auth]) create-endpoint :payload payload)
;          story-name (get-in (json/read-str result) ["CreateResult" "Object" "_refObjectName"])
;          story-ref (get-in (json/read-str result) ["CreateResult" "Object" "_ref"])]
;      (is (str/starts-with? story-name "another"))
;      (let [story-endpoint (second (str/split story-ref #"v2.0"))
;            delete-result (make-request :delete (get-in rally [:auth]) story-endpoint)
;            errors (get-in (json/read-str delete-result) ["OperationResult" "Errors"])]
;        (is (empty? errors))))))

;(deftest find-n-update-test
;  (let [artifact-type "HierarchicalRequirement"
;        query  "&query=(Name = \"DON'T DELETE\")"
;        fetch  "&fetch=ObjectID,ScheduleState"
;        context-oids (context)
;        story-resource "/%s?workspace=/workspace/%s&project=/project/%s"
;        find-endpoint (format (str story-resource fetch) artifact-type (get context-oids :workspace) (get context-oids :project))]
;    (let [result (make-request :get (get-in rally [:auth]) find-endpoint :query query)
;          story-name  (get-in (json/read-str result) ["QueryResult" "Results" 0 "_refObjectName"])
;          story-ref   (get-in (json/read-str result) ["QueryResult" "Results" 0 "_ref"])
;          story-state (get-in (json/read-str result) ["QueryResult" "Results" 0 "ScheduleState"])]
;      (is (str/starts-with? story-name "DON'T DELETE"))
;      (log/info story-ref)
;    (let [target-state (if (= story-state "Defined") "In-Progress" "Defined")
;          update-payload  (format "{\"%s\":{\"ScheduleState\":\"%s\"}}" artifact-type target-state)
;          update-endpoint (second (str/split story-ref #"v2.0"))]
;      (let [update-result (make-request :post (get-in rally [:auth]) update-endpoint :payload update-payload)
;            updated-story-state (get-in (json/read-str update-result) ["OperationResult" "Object" "ScheduleState"])]
;        (is (= updated-story-state target-state)))))))

(deftest create-test
  (let [wi-type    "HierarchicalRequirement"
        name       (random-name)
        est        (float(+ (rand-int 10) 1))
        data       {:Name name :PlanEstimate est}
        result     (create-workitem wi-type data)
        story-name (get-in (json/read-str result) ["CreateResult" "Object" "Name"])
        story-est  (get-in (json/read-str result) ["CreateResult" "Object" "PlanEstimate"])]
    (is (= story-name name))
    (is (= story-est est))))

(deftest read-test
  (let [wi-type    "HierarchicalRequirement"
        query      "(Name = \"DON'T DELETE\")"
        fetch      "ObjectID,ScheduleState"
        result     (read-workitem wi-type query fetch)
        name       (get-in (json/read-str result) ["QueryResult" "Results" 0 "_refObjectName"])]
    (is (= name "DON'T DELETE"))))

(deftest update-test
  (let [wi-type      "HierarchicalRequirement"
        query        "(Name = \"DON'T DELETE\")"
        fetch        "ObjectID,ScheduleState"
        result       (read-workitem wi-type query fetch)
        oid          (get-in (json/read-str result) ["QueryResult" "Results" 0 "ObjectID"])
        state        (get-in (json/read-str result) ["QueryResult" "Results" 0 "ScheduleState"])
        target-state (if (= state "Defined") "In-Progress" "Defined")
        data         {:ScheduleState target-state}
        new-result   (update-workitem wi-type data oid)
        new-state    (get-in (json/read-str new-result) ["OperationResult" "Object" "ScheduleState"])]
    (is (= new-state target-state))))

(deftest delete-test
  (let [wi-type    "HierarchicalRequirement"
        name       (random-name)
        est        (float(+ (rand-int 10) 1))
        data       {:Name name :PlanEstimate est}
        result     (create-workitem wi-type data)
        oid        (get-in (json/read-str result) ["CreateResult" "Object" "ObjectID"])
        deleted    (delete-workitem wi-type oid)
        errors     (get-in (json/read-str deleted) ["OperationResult" "Errors"])]
    (is (empty? errors))))

(deftest bad-creds-test
  (is (thrown? Exception (subscription-info))))

(deftest page-not-found-test
  (is (thrown? Exception (subscription-info))))

(defn -main [& args]
  (let [config (clojure.edn/read-string (slurp "resources/config.edn"))]
    (intern 'clj-rally.core 'rally (connection config)))

  (subscription-info-test)
  (context-test)
  ;(create-n-delete-test)
  ;(find-n-update-test)
  (create-test)
  (read-test)
  (update-test)
  (delete-test)

  (let [config (clojure.edn/read-string (slurp "resources/bad-creds.edn"))]
    (intern 'clj-rally.core 'rally (connection config)))
  (bad-creds-test)

  (let [config (clojure.edn/read-string (slurp "resources/bad-url.edn"))]
    (intern 'clj-rally.core 'rally (connection config)))
  (page-not-found-test))



