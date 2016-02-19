(ns s7p.slave.core
  (:require
   [clojure.core.async :refer [thread close! <!!]]
   [clojure.tools.logging :as log]
   [cheshire.core :as json]
   [org.httpkit.client :as http]
   [s7p.config :refer [advertisers dsps]]
   [s7p.slave.log.bidresponse :as bidresponse]
   [s7p.slave.log.winnotice :as winnotice])
  (:import
   [com.fasterxml.jackson.core JsonParseException]))

(def options {:timeout 100
              :keepalive 3000})

(defn json-request-option [hash]
  (assoc options :as :text :body (json/generate-string hash)))

(defn destruct [{dsp :dsp res :response}]
  (let [{:keys [status body]} @res]
    {:dsp dsp :status status :body body}))

(defn validate [{dsp :dsp status :status body :body}]
  ;; TODO: validate id identity
  (try
   (let [body (and body (json/parse-string body true))
         {:keys [id bidPrice advertiserId]} body
         ret (cond
               (= status 204) {:status :no-bid}
               (not status) {:status :timeout}
               (not (= status 200)) {:status :invalid :reason "response status" :code status}
               (not body) {:status :invalid :reason "no body"}
               (not id) {:status :invalid :invalid "no bid id"}
               (not (instance? String id)) {:status :invalid :reason "id not string" :id id}
               (not bidPrice) {:status :invalid :reason "no bid price"}
               (not (instance? Double bidPrice)) {:status :invalid :reason "bidPrice not double" :bidPrice bidPrice}
               (not advertiserId) {:status :invalid :reason "no advertiser id"}
               (not (instance? String advertiserId)) {:status :invalid :reason "advertiserId not string" :advertiserId advertiserId}
               true   {:status :valid :response body})]
     (assoc ret :dsp dsp))
   (catch JsonParseException e {:status :invalid :reason "invalid JSON"})
   (catch Exception          e {:status :invalid :reason (format "validation process raised unexpected error: %s" e)})))

(defn log-validated [test arg]
  (let [{dsp :dsp res :response status :status} arg]
    (if (= status :valid)
      (bidresponse/log (assoc res :status :valid :dspId (:id dsp) :test test))
      (bidresponse/log (assoc (dissoc arg :dsp)  :dspId (:id dsp) :test test))))
  arg)

(defn succeed? [v]
  (= :valid (:status v)))


(defn over-floor? [floor-price {res :response}]
  (if floor-price
    (<= floor-price (:bidPrice res))
    true))

(defn auction [floor-price resps]
  (case  (count resps)
    0 nil
    1 (let [{dsp :dsp res :response} (first resps)
            fp (or floor-price (:bidPrice res))]
        {:dsp dsp :response res :win-price fp})
    (let [[{dsp :dsp res :response} {win-price :response}]
          (->> resps
               (shuffle)
               (sort-by (comp :bidPrice :response) >)
               (take 2))]
      {:dsp dsp :response res :win-price (+ 1 (:bidPrice win-price))})))

(defn click? [result response]
  (result (.indexOf (map :id advertisers) (:advertiserId response))))

(defn non-nil [f data]
  (if data
    (f data)
    data))

(defn to-winnotice [result {:keys [dsp response win-price]}]
  {:dsp dsp
   :notice {:id (:advertiserId response)
            :price win-price
            :isClick (click? result response)}})

(defn log-winnotice-option [test data]
  (if data
    (let [{dsp :dsp notice :notice} data]
      (winnotice/log (assoc notice :status "auction" :dspId (:id dsp) :test test)))
    (winnotice/log {:status "no auction" :test test})))

(defn winnotice [{dsp :dsp notice :notice}]
  (http/post (:winnotice dsp) (json-request-option notice)))

(defn work [test req result]
  (->> @dsps
       (sequence (comp
                  (map (fn [dsp] {:dsp dsp :response (http/post (:url dsp) (json-request-option req))}))
                  (map destruct)
                  (map validate)
                  (map #(log-validated false %))
                  (filter succeed?)
                  (map (fn [{dsp :dsp res :response}] {:dsp dsp :response res}))
                  (filter #(over-floor? (:floorPrice req) %))))
       (auction (:floorPrice req))
       (non-nil #(to-winnotice result %))
       (log-winnotice-option false)
       (non-nil winnotice)))

(defn worker [c]
  (thread
    (loop []
      (when-let [{:keys [req result]} (<!! c)]
        (work (= 1 (:test req)) req result)
        (recur)))))
