(ns s7p.master
  (:gen-class)
  (:require
   [taoensso.carmine :as car :refer (wcar)]
   [taoensso.carmine.message-queue :as car-mq]
   [clojure.data.json :as json]
   [clojure.core.async :refer [thread close! chan <!!]]
   [s7p.config :as config]
   [s7p.manage :as manage]
   [s7p.core :as core]
))


(def request
  {:req {:id "id"
    :floorPrice 0.1
    :site "http://github.com/KeenS"
    :device "Android"
    :user "user-1"
    :test 1}
   :result [true]})


(defn -main [& args]
 (car/wcar config/redis
           (car/publish "create-dsp" (json/write-str {:id "1" :url config/someurl}))
           (car/publish "delete-dsp" (json/write-str {:id "1"}))
           (loop [i 10]
             (car-mq/enqueue config/queue-name (json/write-str request))
             (if (< 0 i)
               (recur (- i 1))))))
