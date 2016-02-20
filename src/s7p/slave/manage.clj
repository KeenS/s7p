(ns s7p.slave.manage
  (:require [s7p.config :refer [advertisers dsps]]
            [s7p.slave.core :as core]))


(defn add-dsp [dsp]
  (swap! dsps conj dsp))

(defn remove-dsp [id]
  (reset! dsps (remove #(= (:id %) id) @dsps)))

(defn sync-dsps [new-dsps]
  (reset! dsps new-dsps))

(defn make-workers [ch n]
  (doall (map (fn [_] (core/worker ch)) (range n))))
