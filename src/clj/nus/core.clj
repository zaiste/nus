(ns nus.core
  (:use ring.util.response)
  (:require [compojure.handler :as handler]
            [compojure.route :as route]
            [compojure.core :refer [GET POST defroutes]]
            [ring.middleware.edn :refer [wrap-edn-params]]
            [ring.middleware.json :as middleware]
            [datomic.api :as d]))

(def uri "datomic:free://localhost:4334/nus")
(def conn (d/connect uri))

(defn get-posts []
  (let [db (d/db conn)
        posts
        (vec (map #(d/touch (d/entity db (first %)))
               (d/q '[:find ?post
                      :where
                      [?post :post/title]]
                 db)))]
    (response posts)))


(defroutes app-routes
  (GET  "/" [] (resource-response "index.html" {:root "public"}))
  (GET  "/widgets" [] (get-posts))
  (route/resources "/")
  (route/not-found "Page not found"))

(def app
  (-> (handler/api app-routes)
      (middleware/wrap-json-body)
      (middleware/wrap-json-response)))
