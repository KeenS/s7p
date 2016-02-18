(ns s7p.slave.core-test
  (:require [clojure.test :refer :all]
            [s7p.slave.core :refer :all]))

(def dsp1 {:id        "1"
          :url       "http://example.com/api"
          :winnotice "http://example.com/winnotice"})

(def dsp2 {:id        "2"
          :url       "http://example.com/api"
           :winnotice "http://example.com/winnotice"})

(def dsp3 {:id        "3"
          :url       "http://example.com/api"
          :winnotice "http://example.com/winnotice"})

(deftest validate-test
  (testing "`validate`"
    (testing "204 no bid"
      (let [ret (validate {:dsp dsp1 :status 204 :body ""})]
       (is (= :no-bid (:status ret)))))

    (testing "invalid response status"
      (let [ret (validate {:dsp dsp1 :status 201 :body ""})]
       (is (= :invalid (:status ret)))))

    (testing "empty response"
     (let [ret (validate {:dsp dsp1 :status 200 :body ""})]
       (is (= :invalid (:status ret)))))

    (testing "invalid json string"
      (let [ret (validate {:dsp dsp1 :status 200 :body "{"})]
       (is (= :invalid (:status ret)))))

    (testing "valid request"
      (let [ret (validate {:dsp dsp1 :status 200 :body "{\"id\": \"1\", \"bidPrice\": 0.1, \"advertiserId\": \"1\"}"})]
       (is (= :valid (:status ret)))))

    (testing "no id"
      (let [ret (validate {:dsp dsp1 :status 200 :body "{\"bidPrice\": 0.1, \"advertiserId\": \"1\"}"})]
       (is (= :invalid (:status ret)))))

    (testing "no bidPrice"
     (let [ret (validate {:dsp dsp1 :status 200 :body "{\"id\": \"1\", \"advertiserId\": \"1\"}"})]
       (is (= :invalid (:status ret)))))

    (testing "no advertiserId"
      (let [ret (validate {:dsp dsp1 :status 200 :body "{\"id\": \"1\", \"advertiserId\": \"1\"}"})]
       (is (= :invalid (:status ret)))))))

(deftest auction-test
  (testing "`auction`"
    (testing "no valid response"
      (let [fp 4.0
            arg []
            ret (auction fp arg)]
       (is (nil? ret))))
    
    (testing "one valid response with fp, and bidPrice is over the fp"
      (let [fp 4.0
            bid-price 4.1
            arg [{:dsp dsp1 :response {:id "1", :bidPrice bid-price :advertiserId "2"}}]
            ret (auction fp arg)]
       (is ret)
       (is (= dsp1 (:dsp ret)))
       (is (= fp   (:second-price ret)))))

    (testing "one valid response without fp"
      (let [fp nil
            bid-price 4.1
            arg [{:dsp dsp1 :response {:id "1", :bidPrice bid-price :advertiserId "2"}}]
            ret (auction fp arg)]
       (is ret)
       (is (= dsp1 (:dsp ret)))
       (is (= bid-price (:second-price ret)))))

    (testing "more than 1 valid response"
      (let [fp 4.0
            bid-price1 4.1
            bid-price2 4.2
            arg [{:dsp dsp1 :response {:id "1", :bidPrice bid-price1 :advertiserId "2"}}
                 {:dsp dsp2 :response {:id "1", :bidPrice bid-price2 :advertiserId "2"}}]
            ret (auction fp arg)]
        (is ret)
        (is (= dsp2 (:dsp ret)))
        (is (= bid-price1 (:second-price ret)))))))
