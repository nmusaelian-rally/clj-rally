(ns clj-rally.core-test
  (:require [clojure.test :refer :all]
            [clojure.string :as str]
            [clojure.data.json :as json]
            [clj-rally.core :refer :all]
            [clojure.tools.logging :as log]
            [clj-time.core :as t]))


(deftest subscription-info-test
  (let [result (subscription-info)]
    (is (= (get-in result [:workspaces-url]) "/Subscription/1154643/Workspaces"))
    (is (= (get-in result [:sub-id]) 209))
    (is (= (get-in result [:sub-uuid]) "13eb4d62-3c4f-442e-b825-9f6786726d99"))))

(deftest context-test
  (let [result (context)]
    (is (= (get-in result [:workspace]) 1572380957))
    (is (= (get-in result [:project]) 1572381037))))

(deftest create-story-test
  (let [artifact-type "HierarchicalRequirement"
        payload (format "{\"%s\": {\"Name\":\"another clojure toolkit story %s\"}}" artifact-type (str (t/time-now)))
        ;payload {:HierarchicalRequirement {:Name (format "clojure story %s" (str (t/time-now)))}}
        context-oids (context)
        story-resource "/%s/create?workspace=/workspace/%s&project=/project/%s"
        create-endpoint (format story-resource artifact-type (get context-oids :workspace) (get context-oids :project))]
    (let [result (make-request :post (get-in rally [:auth]) create-endpoint :payload payload)
          story-name (get-in (json/read-str result) ["CreateResult" "Object" "_refObjectName"])]
      (is (str/starts-with? story-name "another")))))


(deftest bad-creds-test
  (is (thrown? Exception (subscription-info))))


(deftest page-not-found-test
  (is (thrown? Exception (subscription-info))))


(defn -main [& args]
  (let [config (clojure.edn/read-string (slurp "resources/config.edn"))]
    (intern 'clj-rally.core 'rally (connection config)))

  (subscription-info-test)
  (context-test)
  (create-story-test)

  (let [config (clojure.edn/read-string (slurp "resources/bad-creds.edn"))]
    (intern 'clj-rally.core 'rally (connection config)))
  (bad-creds-test)

  (let [config (clojure.edn/read-string (slurp "resources/bad-url.edn"))]
    (intern 'clj-rally.core 'rally (connection config)))
  (page-not-found-test)
  )



