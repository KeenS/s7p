(ns s7p.slave.core
  (:require
   [clojure.core.async :refer [thread close! <!!]]
   [clojure.tools.logging :as log]
   [cheshire.core :as json]
   [org.httpkit.client :as http]
   [s7p.config :refer [advertisers dsps]]))

(def options {:timeout 100
              :keepalive 3000})


(defn json-request-option [hash]
  (assoc options :body (json/generate-string hash)))

(defn validate [{dsp :dsp res :response}]
  (let [{:keys [status body]} @res
        body (and body (json/parse-string (apply str (map char body)) true))
        {:keys [id bidPrice advertiserId]} body
        ret (cond
              (= status 204) {:status "no-bid"}

              (not (= status 200)) {:status :invalid :reason "response status" :code status}


              body {:status :invalid :reason "no body"}

              (not id) {:status :invalid :invalid "no bid id"}
              (not (instance? String id)) {:status :invalid :reason "id not string" :id id}

              (not bidPrice) {:invalid "no bid price"}
              (not (instance? Double bidPrice)) {:status :invalid :reason "bidPrice not double" :bidPrice bidPrice}

              (not advertiserId) {:invalid "no advertiser id"}
              (not (instance? String advertiserId)) {:status :invalid :reason "advertiserId not string" :advertiserId advertiserId}
              true   {:status :valid :response body})]
    [dsp ret]))

(defn log-validated [arg]
  (let [[{dsp :dsp v :response}] arg]
   (log/info (json/generate-string (assoc v :id (:dsp_id dsp)))))
  arg)

(defn succeed? [v]
  (= :valid (:status v)))


(defn auction [floor-price resps]
  (case  (count resps)
    ;; TODO: log no contest
    0 nil
    1 (let [{dsp :dsp res :response} (first resps)
            fp (or floor-price (:bidPrice res))]
        {:dsp dsp :response res :second-price fp})
    _ (let [[{dsp :dsp res :response} {second-price :response}]
            (->> (conj {:bidPrice floor-price} resps)
                 (shuffle)
                 (take 2))]
        {:dsp dsp :response res :second-price (:bidPrice second-price)})))

(defn click? [result {response :response}]
  (result (.indexOf advertisers (:advertiserId response))))

(defn non-nil [f data]
  (if data
    (f data)
    data))

(defn to-winnotice [result {:keys [dsp response second-price]}]
  {:dsp dsp
   :notice {:id (:advertiserId response)
            :price (+ 1 second-price)
            :isClick (click? result response)}})

(defn log-winnotice-option [data]
  (if data
    (let [{dsp :dsp notice :notice} data]
      (log/info (json/generate-string (assoc notice :status "auction" :dsp_id (:id dsp)))))
    (log/info (json/generate-string {:status "no auction"}))))

(defn winnotice [dsp {notice :notice}]
  (http/post (:winnotice dsp) (json-request-option notice)))

(defn work [req result]
  (->> @dsps
       (sequence (comp
                  (map (fn [dsp] {:dsp dsp :response (http/post (:url dsp) {:as :text} (json-request-option req))}))
                  (map validate)
                  (map log-validated)
                  (filter succeed?)
                  (map (fn [[dsp res]] (dsp (:response res))))))
       (auction (:floorPrice req))
       (non-nil #(to-winnotice result %))
       (log-winnotice-option)
       (non-nil winnotice)))

(defn test-run [req]
  (sequence (comp
             (map (fn [dsp] [dsp (http/post (:url dsp) (json-request-option req))]))
             (map validate))
            @dsps))

(defn worker [c]
  (thread
    (loop []
      (when-let [{:keys [req result]} (<!! c)]
        (if (= 1 (:test req))
          (test-run req)
          (work req result))
        (recur)))))
