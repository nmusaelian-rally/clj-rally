(ns clj-rally.core-test
  (:require [clojure.test :refer :all]
            [clojure.string :as str]
            [clj-rally.core :refer :all]))


(deftest subscription-info-test
  (let [result (subscription-info)]
    (is (= (get-in result [:workspaces-url]) "/Subscription/1154643/Workspaces"))
    (is (= (get-in result [:sub-id]) 209))
    (is (= (get-in result [:sub-uuid]) "13eb4d62-3c4f-442e-b825-9f6786726d99")))
  )


(defn -main [& args]
  (subscription-info-test)
  )





