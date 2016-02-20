(ns s7p.master.web
  (:gen-class)
  (:require
   [clojure.data.csv :as csv]
   [clojure.java.io :as io]
   [compojure.route :as route]
   [compojure.handler :refer [site]]
   [compojure.core :refer [defroutes GET POST DELETE ANY context]]
   [ring.util.response :refer [redirect]]
   [org.httpkit.server :refer :all]
   [zeromq.zmq :as zmq]
   [hiccup.core :refer :all]
   [hiccup.form :refer :all]
   [hiccup.page :refer :all]
   [s7p.config :as config]
   [s7p.master.core :refer :all]))


(def testing (atom 1))
(def queries (atom nil))

(defn running? []
  @queries)

(defn testing? []
  (= @testing 1))

(defn head []
  [:head
   [:title "Home : s7p"]
   (include-css "/style.css")])

(defn header []
  [:header {:class "header"}
   [:h1 {:class "title container"} "s7p - Sexp version of s6p"]])

(defn msg [info]
  (if (:msg info)
    [:div {:class "msg"} (:msg info)]
    ))

(defn qps-change-form []
  (form-to [:post "/control/qps"]
           (text-field {:required true :type "number"} :qps (* 10 @qp100ms))
           (submit-button "change qps")))

(defn mode-toggle-button []
  (if (testing?)
    (form-to {:style "display:inline"} [:post "/control/prd"] (submit-button "production"))
    (form-to {:style "display:inline"} [:post "/control/test"]  (submit-button "test"))))

(defn status-toggle-button []
  (if (running?)
    (form-to {:style "display:inline"} [:post "/control/stop"]  (submit-button "stop"))
    (form-to {:style "display:inline"} [:post "/control/start"] (submit-button "start"))))

(defn status-table []
  [:table {:class "status"}
   [:tr [:th "status"] [:th "mode"] [:th "qps"]]
   [:tr
    [:td (if (running?) "running" "suspended")]
    [:td (if (testing?) "test"    "production")]
    [:td (str (* 10 @qp100ms))]]
   [:tr
    [:td (status-toggle-button)]
    [:td (mode-toggle-button)]
    [:td (qps-change-form)]]])

(defn dsp-create-form []
  (form-to [:post "/dsp/create"]
           [:div
            [:span {:style "display:inline-block"} [:div (label :id              "dsp id")] (text-field {:required true} :id)]
            [:span {:style "display:inline-block"} [:div (label :url        "bid api url")] (text-field {:required true} :url)]
            [:span {:style "display:inline-block"} [:div (label :winnotice"winnotice url")] (text-field {:required true} :winnotice)]
            (submit-button "new dsp")]))

(defn dsp-table []
  [:table {:style "clear: left"}
   [:tr [:th "id"] [:th "api url"] [:th "winnotice url"] [:th ""]]
   (for [dsp @config/dsps]
     [:tr 
      [:td (:id dsp)]
      [:td (:url dsp)]
      [:td (:winnotice dsp)]
      [:td (form-to  [:post "/dsp/delete"] [:input {:name :id :type :hidden :value (:id dsp)}] (submit-button "delete"))]])])

(defn dsp-sync-button []
 (form-to {:style "display:inline"} [:post "/dsp/sync"] (submit-button "sync")))

(defn redirect-with-info [path info]
  (redirect (if (empty? info)
              path
              (str path "?" (ring.util.codec/form-encode info)))))

(defn page [info]
  (html
   (head)
   (header)
   [:div {:class "main container"}
    (msg info)
    (status-table)
    (dsp-create-form)
    (dsp-sync-button)
    (dsp-table)]))

(defn valid? [validate]
  (= (:status validate) :ok))

(defn validate-dsp [id url winnotice]
  (cond
    (not id)                            {:status :error :msg "dsp id cannot be blank"}
    (some #(= (:id %) id) @config/dsps) {:status :error :msg "dsp id cannot be duplicated"}
    (not url)                           {:status :error :msg "url cannot be blank"}
    (not winnotice)                     {:status :error :msg "winnotice url cannot be blank"}
    true                                {:status :ok}))

(def id-count (atom 0))

(defn id-gen []
  (swap! id-count inc)
  (str @id-count))

(defn to-req [line]
  (let [fp (line 1)]
    (let [req      {:req
                    {:id         (id-gen)
                     :site       (line 0)
                     :floorPrice (if (= "NA" fp) nil (Integer. fp))
                     :device     (line 2)
                     :user       (line 3)
                     :test       @testing}
                    :result      (mapv #(Integer. %) (subvec line 4))}]
      (println req)
      req)))

(defn -main [& args]
  (let [context (zmq/zcontext 1)
        in-file (io/reader (first args))
        pub (doto (zmq/socket context :pub)
              (zmq/bind config/command-addr))
        sender (doto (zmq/socket context :push)
                 (zmq/bind config/req-addr))]
    (load-dsps)
    (let [reqs (atom (map to-req (drop 1 (csv/read-csv in-file))))]
      (defroutes routes
        (GET "/" {params :params} (fn [req] (page params)))
        (POST "/dsp/create" {{id :id url :url winnotice :winnotice} :params}
              (fn [req]
                (let [dsp {:id id :url url :winnotice winnotice}
                      v (validate-dsp id url winnotice)]
                  (println "create" dsp)
                  (when (valid? v)
                    (create-dsp pub dsp)
                    (save-dsps))
                  (redirect-with-info "/" v))))
        (POST "/dsp/delete" {{id :id} :params}
              (fn [req]
                (println "delete" id)
                (remove-dsp pub id)
                (save-dsps)
                (redirect-with-info "/" {})))
        (POST "/dsp/sync" []
              (fn [req]
                (println "sync")
                (sync-all-dsp pub)
                (redirect-with-info "/" {})))
        (POST "/control/qps" {{qps :qps} :params}
              (fn [req]
                (println "qps: " qps)
                (change-qps (Integer. qps))
                (redirect-with-info "/" {})))
        (POST "/control/start" []
              (fn [req]
                (println "start")
                (if (not @queries)
                  (reset! queries (start-query sender reqs)))
                (redirect-with-info "/" {})))
        (POST "/control/stop" []
              (fn [req]
                (println "stop")
                (when @queries
                  (stop-query @queries)
                  (reset! queries nil))
                (redirect-with-info "/" {})))
        (POST "/control/test" []
              (fn [req]
                (println "test")
                (reset! testing 1)
                (redirect-with-info "/" {})))
        (POST "/control/prd" []
              (fn [req]
                (println "prd")
                (reset! testing 0)
                (redirect-with-info "/" {})))
        (route/resources "/"))
      (run-server (site #'routes) {:port 8080}))))
