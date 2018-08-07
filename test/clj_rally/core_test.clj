(ns clj-rally.core-test
  (:require [clojure.test :refer :all]
            [clojure.string :as str]
            [clj-rally.core :refer :all]
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
        payload (format "{\"%s\": {\"Name\":\"clojure story %s\"}}" artifact-type (str (t/time-now)))
        context-oids (context)
        create-endpoint (format "/story/create?workspace=/workspace/%s&project=/project/%s" (get context-oids :workspace) (get context-oids :project))
        ]
    (make-request :post (get-in rally [:auth]) create-endpoint payload)))


(defn -main [& args]
  (subscription-info-test)
  (context-test)
  (create-story-test))


