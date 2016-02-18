(ns s7p.slave.core-test
  (:require [clojure.test :refer :all]
            [s7p.slave.core :refer :all]))

(def dsp {:id        "1"
          :url       "http://example.com/api"
          :winnotice "http://example.com/winnotice"})

(deftest validate-test
  (testing "`validate`"
    (let [ret (validate {:dsp dsp :status 204 :body ""})]
      (is (= :no-bid (:status ret))
          "204 no bid"))

    (let [ret (validate {:dsp dsp :status 201 :body ""})]
      (is (= :invalid (:status ret))
          "invalid response status"))

    (let [ret (validate {:dsp dsp :status 200 :body ""})]
      (is (= :invalid (:status ret))
          "empty response"))

    (let [ret (validate {:dsp dsp :status 200 :body "{"})]
      (is (= :invalid (:status ret))
          "invalid json string"))

    (let [ret (validate {:dsp dsp :status 200 :body "{\"id\": \"1\", \"bidPrice\": 0.1, \"advertiserId\": \"1\"}"})]
      (is (= :valid (:status ret))
          "valid request"))

    (let [ret (validate {:dsp dsp :status 200 :body "{\"bidPrice\": 0.1, \"advertiserId\": \"1\"}"})]
      (is (= :invalid (:status ret))
          "no id"))

    (let [ret (validate {:dsp dsp :status 200 :body "{\"id\": \"1\", \"advertiserId\": \"1\"}"})]
      (is (= :invalid (:status ret))
          "no bidPrice"))

    (let [ret (validate {:dsp dsp :status 200 :body "{\"id\": \"1\", \"advertiserId\": \"1\"}"})]
      (is (= :invalid (:status ret))
          "no advertiserId"))
))
