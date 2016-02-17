(ns s7p.core
  (:require [clojure.data.json :as json]
            [org.httpkit.client :as http]
            [clojure.core.async :refer [thread close!  <!!]]
            [s7p.config :refer [advertisers dsps]]))

(def options {:timeout 100
              :keepalive 3000})


(defn json-request-option [hash]
  (assoc options :body (json/write-str hash)))

(defn validate [[dsp res]]
  (let [{:keys [status body]} @res
        body (and body (json/read-str body :key-fn keyword :eof-error? false))
        {:keys [id bidPrice advertiserId]} body
        ret (cond
              (= status 204) {:nobid "no bid"}

              (not (= status 200)) {:invalid "not succeed" :status status}


              body {:invalid "no body"}

              (not id) {:invalid "no id"}
              (not (instance? String id)) {:invalid "id not string" :id id}

              (not bidPrice) {:invalid "no bid price"}
              (not (instance? Double bidPrice)) {:invalid "bidPrice not double" :bidPrice bidPrice}

              (not advertiserId) {:invalid "no advertiser id"}
              (not (instance? String advertiserId)) {:invalid "advertiserId not string" :advertiserId advertiserId}
              true   {:valid body})]
    [dsp ret]))

(defn succeed? [[_ res]]
  (:valid res))


(defn pick-winner-and-second-price [floor-price resps]
  (case  (count resps)
    ;; TODO: log no contest
    0 nil
    1 (let [res (first resps)
            fp (or floor-price (:bidPrice res))]
        [res fp])
    _ (let [[res second-price] (->> (conj {:bidPrice floor-price} resps)
                             (shuffle)
                             (take 2))]
        [res (:bidPrice second-price)])))

(defn click? [result response]
  (result (.indexOf advertisers (:advertiserId response))))

(defn winnotice [request result [[dsp response] second-price]]
  ;; TODO: log
  (println (assoc response :id (:id dsp)))

  (http/post (:winnotice dsp)
             (json-request-option
              {:id (:advertiserId response)
               :price (+ 1 second-price)
               :isClick (click? result response)})))

(defn work [req result]
  (let [xf (comp
                  (map (fn [dsp] [dsp (http/post (:url dsp) (json-request-option req))]))
                  (map validate)
                  ;; TODO: log validated responses
                  (filter succeed?)
                  (map (fn [[_ res]] (:valid res))))]
          (some->> (sequence xf @dsps)
                   (pick-winner-and-second-price (:floorPrice req))
                   (winnotice req result))))

(defn test-run [req]
  (let [xf (comp
            (map (fn [dsp] [dsp (http/post (:url dsp) (json-request-option req))]))
            (map validate))]
          (sequence xf @dsps)))

(defn worker [c]
  (thread
    (loop []
      (when-let [{:keys [req result]} (<!! c)]
        (if (= 1 (:test req))
          (test-run req)
          (work req result))
        (recur)))))
