(ns s7p.core
  (:require [clojure.data.json :as json]
            [org.httpkit.client :as http]
            [clojure.core.async :as a]))

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

(defn validate [{:keys [status body error]}]
  ;; FIXME: implement
  (and
   (= status 200)
   ;; TODO: treat 204 and the others

   (:id body)
   ;; and string

   (:bidPrice body)
   ;; and double

   (:advertiserId body)
   ;; and string

   )
  {:valid body}
  )

(defn succeed? [res]
  ;; FIXME: implement
  {:valid res}
  true
  )


(defn argmaxes-and-second [default key col]
  ;; FIXME: implement
  )

(defn pick-winner-and-second-price [floor-price resps]
  ;; TODO: second price
  (->> resps
       ;; shuffle
       ;; sort
       ;; take2
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



(defn floor-price [req]
  ;; FIXME: implement
  0
  )

(defn gen-request [n]
  (let [c (a/chan)]
    (a/go-loop [i n]
      (a/>! c request)
      (if (< 0 i)
        (recur (- i 1))
        (a/close! c)))
    c))

(defn process [chan]
  (let [sig (a/chan)]
   (a/go-loop []
     (if-let [{:keys [req result]} (a/<! chan)]
       (do
         (a/go
           (some->> (sequence (xf (assoc options :body (json/write-str req)))
                              urls)
                    (pick-winner-and-second-price (floor-price req))
                    ;; log winner
                    (winnotice req result)))
         (recur))
       (do
         (a/close! sig))))))


(defn -main []
  (let [c (gen-request 1)
        sig (process c)]
    (println (a/<!! sig))))
