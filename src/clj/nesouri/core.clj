(ns nesouri.core
  (:require [clojure.java.io :as io]
            [compojure.core :refer :all]
            [compojure.route :as route]
            [ring.util.response :as response]
            [ring.middleware.defaults :refer [wrap-defaults site-defaults]]
            [ring.middleware.reload :refer [wrap-reload]]
            [nesouri.database :as db]))

(defn generate-response [data & [status]]
  {:status (or status 200)
   :headers {"Content-Type" "application/edn"}
   :body (pr-str data)})

(defn optional-resource [lookup]
  (try
    (generate-response (lookup))
    (catch IllegalArgumentException e
      (response/not-found (.getMessage e)))))

(defroutes app-routes
  (GET "/" [] (io/resource "public/html/index.html"))
  (GET "/random-track" []
       (generate-response (db/random-track)))
  (GET "/games" {{limit :limit offset :offset} :params}
       (generate-response (db/list-games limit offset)))
  (GET "/games/:game_id" [game_id]
       (optional-resource #(db/show-game game_id)))
  (GET "/artists/:artist_id" [artist_id]
       (optional-resource #(db/show-artist artist_id)))
  (GET "/organizations/:organization_id" [organization_id]
       (optional-resource #(db/show-organization organization_id)))
  (route/resources "/" {:root "public/html"}))

(def app
  (wrap-defaults (wrap-reload #'app-routes) site-defaults))
