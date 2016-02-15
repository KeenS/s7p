(ns s7p.core
  (:require [clojure.data.json :as json]
            [org.httpkit.client :as http]
            [core.async :refer [go]]))

(def options {:timeout 100
              :keepalive 3000})

(def urls ["http://dsp-no-bid.compe"])

(def request
  {:id "id"
   :floorPrice 0.1
   :site "http://github.com/KeenS"
   :device "Android"
   :user "user-1"
   :test 1})

(defn validate [{:keys status body error}]
  ;; FIXME: implement
  (and
   (= status 200)
   ;; TODO: treat 204 and the others

   (:id res)
   ;; and string

   (:bidPrice res)
   ;; and double

   (:advertiserId res)
   ;; and string

   )
  {:valid res}
  )

(defn succeed? [res]
  ;; FIXME: implement
  {:valid}
  true
  )


(defn argmaxes-and-second [default key col]
  ;; FIXME: implement
  )

(defn pick-winner-and-second-price [floor-price resps]
  ;; TODO: second price
  (->> resps
       (argmaxes-and-second floor-price :bidPrice)
       ;; branch the size of maxes
       (rand-nth)))

(defn winnotice [request result response]
  ;; FIXME: implement
  (println response)
  (http/post (:winnotic-url response)
             (assoc merge {:id "id" :price 0.1 :isClick true}))
  )

(defn xf [options]
  (comp
   (map (fn [url] (http/post url (conj options))))
   (map (fn [future] @future))
   (map validate)
   ;; log validated responses
   (filter succeed?)
   (map :res)))



(defn gen-request [n]
  (let [c (chan)])
  (go
    (loop [i n]
      (>! c request)
      (if (< 0 i)
        (recur (- i 1)))))
  c)

(defn process [chan]
  (loop []
    (let [{:keys [req result]} (<! chan)]
      (go
        (some->> (sequence (xf (assoc options :body (json/write-str req)))
                       urls)
             (pick-winner-and-second-price (floor-price req))
             ;; log winner
             (winnotice req result))))
    (recur)))


(defn -main []
  (let [c (gen-request 1)]
      (process chan)))
