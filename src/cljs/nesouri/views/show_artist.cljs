(ns nesouri.views.show-artist
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [cljs.core.async :as async :refer [put! chan alts!]]
            [om.core :as om :include-macros true]
            [om-tools.dom :as dom :include-macros true]
            [om-tools.core :refer-macros [defcomponent]]
            [om-bootstrap.grid :as g]
            [om-bootstrap.panel :as p]
            [om-bootstrap.table :refer [table]]
            [om-sync.util :refer [edn-xhr]]))

(defcomponent game-view
  [game owner]
  (render-state
   [_ state]
   (dom/tr
    (dom/td (dom/a {:href (str "#/details/" (:game_id game))} (:title game)))
    (dom/td (or (:date game) (:year game)))
    (dom/td (:developer game))
    (dom/td (:publisher game)))))

(defcomponent show-artist
  [app owner]
  (will-mount
   [_]
   (edn-xhr {:method :get
             :url (str "artists/" (om/get-state owner :artist_id))
             :on-complete #(om/set-state! owner :metadata %)}))
  (render-state
   [_ {:keys [metadata]}]
   (g/grid {:style { :bottom "30px"}}
           (g/row {:class "show-grid"}
                  (g/col {:lg 1}
                         (dom/a {:href "javascript:history.back()"} "Back"))
                  (g/col {:lg 4}
                         (dom/h1 (:name metadata))))
           (g/row {:class "show-grid"}
                  (g/col {:lg 10 :lg-offset 1}
                         (p/panel {:list-group
                                   (table {:bordered? true :condensed? false :hover? true}
                                          (apply dom/tbody
                                                 (om/build-all game-view (:games metadata))))}))))))
