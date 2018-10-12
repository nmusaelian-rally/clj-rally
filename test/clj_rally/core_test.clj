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
  (let [result (subscription-info-test)]
    (is (= (get-in result [:workspaces-url]) "/Subscription/1154643/Workspaces"))
    (is (= (get-in result [:sub-id]) 209))
    (is (= (get-in result [:sub-uuid]) "13eb4d62-3c4f-442e-b825-9f6786726d99"))))

(deftest context-test
  (let [result (context)]
    (is (= (get-in result [:workspace]) 1572380957))
    (is (= (get-in result [:project]) 1572381037))))


;(deftest create-story
;  (let [wi-type    "HierarchicalRequirement"
;        name       (random-name)
;        est        (float(+ (rand-int 10) 1))
;        data       {:Name name :PlanEstimate est}
;        result     (create-workitem wi-type data)
;        story-name (get-in (json/read-str result) ["CreateResult" "Object" "Name"])
;        story-est  (get-in (json/read-str result) ["CreateResult" "Object" "PlanEstimate"])]
;    (is (= story-name name))
;    (is (= story-est est))))

(deftest read-story
  (let [wi-type    "HierarchicalRequirement"
        query      "(Name = \"DON'T DELETE\")"
        fetch      "ObjectID,ScheduleState"
        result     (read-workitem wi-type query fetch)
        name       (get-in (json/read-str result) ["QueryResult" "Results" 0 "_refObjectName"])]
    (is (= name "DON'T DELETE"))))

;(deftest update-story
;  (let [wi-type      "HierarchicalRequirement"
;        query        "(Name = \"DON'T DELETE\")"
;        fetch        "ObjectID,ScheduleState"
;        result       (read-workitem wi-type query fetch)
;        oid          (get-in (json/read-str result) ["QueryResult" "Results" 0 "ObjectID"])
;        state        (get-in (json/read-str result) ["QueryResult" "Results" 0 "ScheduleState"])
;        target-state (if (= state "Defined") "In-Progress" "Defined")
;        data         {:ScheduleState target-state}
;        new-result   (update-workitem wi-type data oid)
;        new-state    (get-in (json/read-str new-result) ["OperationResult" "Object" "ScheduleState"])]
;    (is (= new-state target-state))))

;(deftest delete-story
;  (let [wi-type    "HierarchicalRequirement"
;        name       (random-name)
;        est        (float(+ (rand-int 10) 1))
;        data       {:Name name :PlanEstimate est}
;        result     (create-workitem wi-type data)
;        oid        (get-in (json/read-str result) ["CreateResult" "Object" "ObjectID"])
;        deleted    (delete-workitem wi-type oid)
;        errors     (get-in (json/read-str deleted) ["OperationResult" "Errors"])]
;    (is (empty? errors))))

;(deftest create-pi
;  (let [pi-type    "PortfolioItem/Feature"
;        name       (str "DON'T DELETE!"(random-name))
;        data       {:Name name}
;        result     (create-workitem pi-type data)
;        pi-name    (get-in (json/read-str result) ["CreateResult" "Object" "Name"])
;        pi-ref     (get-in (json/read-str result) ["CreateResult" "Object" "_ref"])
;        short-ref  (second (str/split pi-ref #"v2.0"))
;        (defn report-income
;          [income]
;          (let [total (reduce + (vals income))
;                notok (atom 0)]
;            (if (contains? income :bribes) (reset! notok (:bribes income)))
;            (if (contains? income :embezz) (reset! notok (+ (deref notok) (:embezz income))))
;            (let [ok (- total (deref notok))]
;              ok)))
;
;        (println (report-income {:salary 2000 :rental 1000 :divident 100 :bribes 5000 :embezz :2000}))    wi-type     "HierarchicalRequirement"
;        query       "(Name = \"DON'T DELETE\")"
;        fetch       "ObjectID,Feature"
;        result      (read-workitem wi-type query fetch)
;        oid         (get-in (json/read-str result) ["QueryResult" "Results" 0 "ObjectID"])
;        data         {:PortfolioItem short-ref}
;        new-result   (update-workitem wi-type data oid)
;        new-parent   (get-in (json/read-str new-result) ["OperationResult" "Object" "Feature" "_refObjectName"])]
;    (is (= pi-name name))
;    (is (= new-parent pi-name))))

;(deftest create-pi
;  (let [pi-type    "PortfolioItem/Feature"
;        name       (str "DON'T DELETE!"(random-name))
;        data       {:Name name}
;        result     (create-workitem pi-type data)
;        pi-name    (get-in (json/read-str result) ["CreateResult" "Object" "Name"])
;        pi-ref     (get-in (json/read-str result) ["CreateResult" "Object" "_ref"])
;        short-ref  (second (str/split pi-ref #"v2.0"))
;        wi-type     "HierarchicalRequirement"
;        query       "(Name = \"DON'T DELETE\")"
;        fetch       "ObjectID,Feature"
;        result      (read-workitem wi-type query fetch)
;        oid         (get-in (json/read-str result) ["QueryResult" "Results" 0 "ObjectID"])
;        data         {:PortfolioItem short-ref}
;        new-result   (update-workitem wi-type data oid)
;        new-parent   (get-in (json/read-str new-result) ["OperationResult" "Object" "Feature" "_refObjectName"])]
;    (is (= pi-name name))
;    (is (= new-parent pi-name))))

;(deftest bad-creds-test
;  (is (thrown? Exception (subscription-info))))
;
;(deftest page-not-found-test
;  (is (thrown? Exception (subscription-info))))

(defn -main [& args]
  (let [config (clojure.edn/read-string (slurp "resources/config.edn"))]
    (intern 'clj-rally.core 'rally (connection config)))


  (subscription-info-test)
  (context-test)
  ;(create-story)
  ;(read-story)
  ;(update-story)
  ;(delete-story)
  ;(create-pi)

  ;(let [config (clojure.edn/read-string (slurp "resources/bad-creds.edn"))]
  ;  (intern 'clj-rally.core 'rally (connection config)))
  ;(bad-creds-test)
  ;
  ;(let [config (clojure.edn/read-string (slurp "resources/bad-url.edn"))]
  ;  (intern 'clj-rally.core 'rally (connection config)))
  ;(page-not-found-test)
  )



