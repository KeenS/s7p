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
              :keepalive 3000
              :headers {"Content-Type" "application/json"}})

(defn json-request-option [hash]
  (assoc options :as :text :body (json/generate-string hash)))

(defn destruct [{dsp :dsp res :response}]
  (let [{:keys [status body]} @res]
    {:dsp dsp :status status :body body}))

(defn over-floor? [floor-price bid-price]
  (if floor-price
    (<= floor-price bid-price)
    true))

(defn validate [request {dsp :dsp status :status body :body}]
  (let [ret (cond
              (= status 204)
              {:status :no-bid}
              
              (not status)
              {:status :timeout}
              
              (not (= status 200))
              {:status :invalid :reason "response status" :code status :body body}
              
              :else
              (try
                (let [body (and body (json/parse-string body true))
                      {:keys [id bidPrice advertiserId]} body]
                  (cond
                    (not body)
                    {:status :invalid :reason "no body"}
                    
                    (not id)
                    {:status :invalid :invalid "no bid id"}
                    
                    (not (instance? String id))
                    {:status :invalid :reason "id not string" :id id}
                    
                    (not (= id (:id request)))
                    {:status :invalid :reason "bid id differs from request-id" :id id :request-id (:id request)}
                    
                    (not bidPrice)
                    {:status :invalid :reason "no bid price"}
                    
                    (not (instance? Number bidPrice))
                    {:status :invalid :reason "bidPrice not double" :bidPrice bidPrice}
                    
                    (not (over-floor? (:floorPrice request) bidPrice))
                    {:status :invalid :reason "bid price under floor price" :floorPrice (:floorPrice request) :bidPrice bidPrice}
                    
                    (not advertiserId)
                    {:status :invalid :reason "no advertiser id"}
                    
                    (not (instance? String advertiserId))
                    {:status :invalid :reason "advertiserId not string" :advertiserId advertiserId}

                    :else
                    {:status :valid :response body}))
                (catch JsonParseException e {:status :invalid :reason "invalid JSON" :body body})
                (catch Exception          e {:status :invalid :reason (format "validation process raised unexpected error: %s" e)})))]
    (assoc ret :dsp dsp)))

(defn log-validated [test arg]
  (let [{dsp :dsp res :response status :status} arg]
    (if (= status :valid)
      (bidresponse/log (assoc res :status :valid :dspId (:id dsp) :test test))
      (bidresponse/log (assoc (dissoc arg :dsp)  :dspId (:id dsp) :test test))))
  arg)

(defn succeed? [v]
  (= :valid (:status v)))


(defn auction [floor-price resps]
  (let [floor-price (and floor-price floor-price)]
   (case  (count resps)
     0 nil
     1 (let [{dsp :dsp res :response} (first resps)
             fp (or floor-price (:bidPrice res))]
         {:dsp dsp :response res :win-price (float (/ fp 1000))})
     (let [[{dsp :dsp res :response} {win-price :response}]
           (->> resps
                (shuffle)
                (sort-by (comp :bidPrice :response) >)
                (take 2))]
       {:dsp dsp :response res :win-price (float (/ (:bidPrice win-price) 1000))}))))

(defn click? [result response]
  (result (.indexOf (map :id advertisers) (:advertiserId response))))

(defn non-nil [f data]
  (if data
    (f data)
    data))

(defn to-winnotice [result {:keys [dsp response win-price]}]
  {:dsp dsp
   :notice {:id (:id response)
            :price win-price
            :isClick (click? result response)}})

(defn log-winnotice-option [test request-id data]
  (if data
    (let [{dsp :dsp notice :notice} data]
      (winnotice/log (assoc notice :status "auction" :dspId (:id dsp) :test test)))
    (winnotice/log {:status "no auction" :id request-id :test test}))
  data)

(defn winnotice [{dsp :dsp notice :notice}]
  (http/post (:winnotice dsp) (json-request-option notice))
  )

(defn work [test req result]
  (->> @dsps
       (map (fn [dsp] {:dsp dsp :response (http/post (:url dsp) (json-request-option req))}))
       (sequence (comp
                  (map destruct)
                  (map #(validate req %))
                  (map #(log-validated test %))
                  (filter succeed?)
                  (map (fn [{dsp :dsp res :response}] {:dsp dsp :response res}))))
       (auction (:floorPrice req))
       (non-nil #(to-winnotice result %))
       (log-winnotice-option test (:id req))
       (non-nil winnotice)))

(defn worker [c]
  (thread
    (loop []
      (when-let [{:keys [req result]} (<!! c)]
        (work (:test req) req result)
        (recur)))))
