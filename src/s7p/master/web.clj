(ns s7p.master.web
  (:require [compojure.route :refer [files not-found]]
            [compojure.handler :refer [site]]
            [compojure.core :refer [defroutes GET POST DELETE ANY context]]
            [ring.util.response :refer [redirect]]
            [org.httpkit.server :refer :all]
            [zeromq.zmq :as zmq]
            [hiccup.core :refer :all]
            [hiccup.form :refer :all]
            [s7p.config :as config]
            [s7p.master.core :refer [remove-dsp create-dsp change-qps start-query stop-query qp100ms timer]]))



(defn page []
  (html
   (form-to [:post "/dsp/create"]
            [:div
             "dsp id"        [:input {:name :id}]
             "bid api url"   [:input {:name :url}]
             "winnotice url" [:input {:name :winnotice}]
             ]
            (submit-button "new dsp"))
   (form-to [:post "/control/qps"]
            [:div
             "qps"        [:input {:name :qps :value (* 10 @qp100ms)}]
             ]
            (submit-button "change qps"))
   (form-to [:post "/control/start"]
            (submit-button "start"))
   (form-to [:post "/control/stop"]
            (submit-button "stop"))
   [:ul
    (for [dsp @config/dsps]
      [:li (form-to [:post "/dsp/delete"]
                    ;; TODO: visibility
                    [:input {:name :id :visibility :hidden :value (:id dsp)}]
                    [:ul
                     [:li (:id dsp)]
                     [:li (:url dsp)]
                     [:li (:winnotice dsp)]
                     (submit-button "delete")])])]))


(defn -main [& args]
  (let [context (zmq/zcontext 1)
        queries (atom nil)
        ;; TODO:from file
        reqs]
    (with-open [;in-file (io/reader (first args))
                pub (doto (zmq/socket context :pub)
                      (zmq/bind config/command-addr))
                sender (doto (zmq/socket context :push)
                         (zmq/bind config/req-addr))]
      (defroutes routes
        (GET "/" [] (fn [req] (page)))
        (POST "/dsp/create" {{id :id url :url winnotice :winnotice} :params}
              (fn [req]
                (create-dsp pub {:id id :url url :winnotice winnotice})
                (redirect "/")))
        (POST "/dsp/delete" {{id :id} :params}
              (fn [req]
                (remove-dsp pub id)
                (redirect "/")))
        (POST "/control/qps" {{qps :qps} :params}
              (fn [req]
                (change-qps (Integer. qps))
                (redirect "/")))
        (POST "/control/start" []
              (fn [req]
                (println "start")
                (if (not @queries)
                  (reset queries (start-query sender reqs)))
                (redirect "/")))
        (POST "/control/stop" []
              (fn [req]
                (println "stop")
                (when @queries
                  (stop-query @queries)
                  (reset queries nil))
                (redirect "/"))))
      (run-server (site #'routes) {:port 8080}))))
