(ns ^:figwheel-always nesouri.core
    (:require-macros [cljs.core.async.macros :refer [go]])
    (:require [cljs.core.async :as async :refer [put! chan alts!]]
              [om.core :as om :include-macros true]
              [nesouri.views.search :refer [search-view]]
              [nesouri.views.player :refer [player-view]]
              [nesouri.views.details :refer [game-details-view]]
              [nesouri.views.show-artist :refer [show-artist]]
              [nesouri.views.show-organization :refer [show-organization]]
              [secretary.core :as sec :include-macros true]
              [goog.events :as events]
              [goog.history.EventType :as EventType])
    (:import goog.History))

(enable-console-print!)

(sec/set-config! :prefix "#")

(let [history (History.)]
  (goog.events/listen history EventType/NAVIGATE #(-> % .-token sec/dispatch!))
  (doto history (.setEnabled true)))

(def app-state
  (atom {:games []}))

(let [player-chan (chan)
      options {:target (. js/document (getElementById "app"))
               :state {:player-chan player-chan}}]

  (sec/defroute index-page "/" []
    (sec/dispatch! "/search"))

  (sec/defroute search-page "/search" []
    (om/root search-view app-state options))

  (sec/defroute artist-page "/artist/:artist_id" [artist_id]
    (om/root show-artist app-state
             (update-in options [:state] conj {:artist_id artist_id})))

  (sec/defroute organization-page "/organizations/:organization_id" [organization_id]
    (om/root show-organization app-state
             (update-in options [:state] conj {:organization_id organization_id})))

  (sec/defroute details-page "/details/:game_id" [game_id]
    (om/root game-details-view app-state
             (update-in options [:state] conj {:game_id game_id})))

  (om/root player-view app-state
           {:target (.getElementById js/document "player")
            :state {:player-chan player-chan}}))

(let [current-hash (.. js/window -location -hash)]
  (if (empty? current-hash)
    (sec/dispatch! "/")
    (sec/dispatch! (.substring current-hash 1))))
