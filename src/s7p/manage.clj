(ns s7p.manage
  (:require [s7p.config :refer [advertisers dsps]]))


(defn add-dsp [dsp]
  (swap! dsps conj dsp))

(defn remove-dsp [id]
  (reset! dsps (remove #(= (:id %) id) @dsps)))

