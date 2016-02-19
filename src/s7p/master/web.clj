(ns s7p.master.web
  (:require
   [clojure.data.csv :as csv]
   [clojure.java.io :as io]
   [compojure.route :refer [files not-found]]
   [compojure.handler :refer [site]]
   [compojure.core :refer [defroutes GET POST DELETE ANY context]]
   [ring.util.response :refer [redirect]]
   [org.httpkit.server :refer :all]
   [zeromq.zmq :as zmq]
   [hiccup.core :refer :all]
   [hiccup.form :refer :all]
   [s7p.config :as config]
   [s7p.master.core :refer [remove-dsp create-dsp change-qps start-query stop-query qp100ms timer]]))


(def testing? (atom 1))

(defn page []
  (html
   (form-to [:post "/dsp/create"]
            [:div
             [:span {:style "display:inline-block"} [:div "dsp id"]        [:input {:name :id}]]
             [:span {:style "display:inline-block"} [:div "bid api url"]   [:input {:name :url}]]
             [:span {:style "display:inline-block"} [:div "winnotice url"] [:input {:name :winnotice}]]
             (submit-button "new dsp")])
   (form-to [:post "/control/qps"]
            [:div
             "qps"        [:input {:name :qps :value (* 10 @qp100ms)}]
             (submit-button "change qps")])
   [:div "mode: " (if (= 1 @testing?) "test" "prd")]
   [:div
    [:span "status: "]
    [:ul {:style "list-style: none"}
          [:li {:style "float: left"} (form-to [:post "/control/start"] (submit-button "start"))]
          [:li {:style "float: left"} (form-to [:post "/control/stop"]  (submit-button "stop"))]]]
   [:ul
    (for [dsp @config/dsps]
      [:li (form-to [:post "/dsp/delete"]
                    ;; TODO: visibility
                    [:input {:name :id :visibility :hidden :value (:id dsp)}]
                    [:ul {:style "list-style: none"}
                     [:li {:style "float: left"} (:id dsp)]
                     [:li {:style "float: left"} (:url dsp)]
                     [:li {:style "float: left"} (:winnotice dsp)]
                     (submit-button "delete")])])]))


(defn to-req [line]
  (let [fp (line 1)]
   {:req
    {:site       (line 0)
     :floorPrice (if (= "NA" fp) nil (Integer. fp))
     :device     (line 2)
     :user       (line 3)
     :test       @testing?}
    :result      (mapv #(Integer. %) (subvec line 4))}))


(defn -main [& args]
  (let [context (zmq/zcontext 1)
        queries (atom nil)
        ;; somehow cannot use with-open
        in-file (io/reader (first args))
        pub (doto (zmq/socket context :pub)
              (zmq/bind config/command-addr))
        sender (doto (zmq/socket context :push)
                 (zmq/bind config/req-addr))]
    (let [reqs (atom (map to-req (drop 1 (csv/read-csv in-file))))]
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
                  (reset! queries (start-query sender reqs)))
                (redirect "/")))
        (POST "/control/stop" []
              (fn [req]
                (println "stop")
                (when @queries
                  (stop-query @queries)
                  (reset! queries nil))
                (redirect "/")))
        (POST "/mode/test" []
              (fn [req]
                (reset! testing? 1)
                (redirect "/")))
        (POST "/mode/prd" []
              (fn [req]
                (reset! testing? 0)
                (redirect "/"))))
      (run-server (site #'routes) {:port 8080}))))
