(ns s7p.core
  (:require [clojure.data.json :as json]
            [org.httpkit.client :as http]
            [clojure.core.async :refer [thread chan close!  <!! >!!]])
  (:gen-class)
  )

(def options {:timeout 100
              :keepalive 3000})

(def someurl "")

(def dsps (atom
           [{:id "1"
             :url someurl
             :winnotice someurl}
            {:id "2"
             :url someurl
             :winnotice someurl}
            {:id "3"
             :url someurl
             :winnotice someurl}
            {:id "4"
             :url someurl
             :winnotice someurl}
            {:id "5"
             :url someurl
             :winnotice someurl}
            {:id "6"
             :url someurl
             :winnotice someurl}
            {:id "7"
             :url someurl
             :winnotice someurl}
            ]))

(def advertisers [{:id "1"} {:id "2"} {:id "3"} {:id "4"}])

(def request
  {:req {:id "id"
    :floorPrice 0.1
    :site "http://github.com/KeenS"
    :device "Android"
    :user "user-1"
    :test 1}
   :result [true]})

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
  (println response)

  (http/post  (:winnotice dsp)
             ;; FIXME: options
             {:id (:advertiserId response)
              :price (+ 1 second-price)
              :isClick (click? result response)}))

(defn gen-request [n]
  (let [c (chan)]
    (thread
      (loop [i n]
        (>!! c request)
        (if (< 0 i)
          (recur (- i 1))
          (close! c))))
    c))

(defn process [c]
  (thread
    (loop []
      (when-let [{:keys [req result]} (<!! c)]
        (let [xf (comp
                  (map (fn [dsp] [dsp (http/post (:url dsp) (assoc options :body (json/write-str req)))]))
                  (map validate)
                  ;; TODO: log validated responses
                  (filter succeed?)
                  (map (fn [[_ res]] (:valid res))))]
          (some->> (sequence xf @dsps)
                   (pick-winner-and-second-price (:floorPrice req))
                   (winnotice req result)
                   )
          (recur))))))


(defn -main [& args]
  (time (let [c (gen-request 1000)]
          (doseq [sig (vec (map (fn [_] (process c)) (range 64)))]
            (<!! sig)))))
