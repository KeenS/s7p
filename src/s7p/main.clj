(ns s7p.main
  (:require [s7p.core :refer [worker]]
            [clojure.core.async :refer [thread chan close!  <!! >!!]])
  (:gen-class))


(def request
  {:req {:id "id"
    :floorPrice 0.1
    :site "http://github.com/KeenS"
    :device "Android"
    :user "user-1"
    :test 1}
   :result [true]})


(defn gen-request [n]
  (let [c (chan)]
    (thread
      (loop [i n]
        (>!! c request)
        (if (< 0 i)
          (recur (- i 1))
          (close! c))))
    c))

(defn -main [& args]
  (time (let [c (gen-request 10)]
          (doseq [sig (vec (map (fn [_] (worker c)) (range 64)))]
            (<!! sig)))))
