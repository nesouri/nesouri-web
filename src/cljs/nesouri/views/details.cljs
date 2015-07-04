(ns nesouri.views.details
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [cljs.core.async :as async :refer [put! chan alts!]]
            [om.core :as om :include-macros true]
            [om-tools.dom :as dom :include-macros true]
            [om-tools.core :refer-macros [defcomponent]]
            [om-bootstrap.grid :as g]
            [om-bootstrap.panel :as p]
            [om-sync.util :refer [edn-xhr]]))

(defcomponent track-view
  [track owner]
  (render-state
   [_ state]
   (dom/li
    (dom/a {:className "list-group-item"
            :on-click #(put! (:player-chan state) [(:track track) (:game_id state)])
            :style {:white-space "nowrap"
                    :overflow "hidden"
                    :text-overflow "ellipsis"}}
           (:title track)))))

(defcomponent game-details-view
  [app owner]
  (will-mount
   [_]
   (edn-xhr {:method :get
             :url (str "games/" (om/get-state owner :game_id))
             :on-complete #(om/set-state! owner :metadata %)}))
  (render-state
   [_ {:keys [game_id player-chan metadata]}]
   (g/grid {:style { :bottom "30px"}}
           (g/row {:class "show-grid"}
                  (g/col {:lg 1}
                         (dom/a {:href "javascript:history.back()"} "Back"))
                  (g/col {:lg 5}
                         (dom/div {:className "media"}
                                  (dom/h3 (:title metadata))
                                  (dom/span {:className "media-left"}
                                            (dom/img {:src (:coverart metadata) :className "media-object"}))
                                  (dom/div {:className "media-body"}
                                           (dom/dl
                                            (dom/dt "Release date")
                                            (dom/dd (:date metadata))
                                            (dom/dt "Developer")
                                            (dom/dd (dom/a {:href (str "#/organizations/" (:developer_id metadata))} (:developer metadata)))
                                            (dom/dt "Publisher:")
                                            (dom/dd (dom/a {:href (str "#/organizations/" (:publisher_id metadata))} (:publisher metadata)))
                                            (dom/dt "Links:")
                                            (dom/dd
                                             (dom/ul
                                              (map (fn [link]
                                                     (dom/li (dom/a {:href (:url link)} (:name link))))
                                                   (:game_links metadata))))))))
                  (g/col {:lg 6}
                         (p/panel {:list-group (apply dom/ol {:className "list-group" :style {:listStyle "decimal-leading-zero inside"}}
                                                      (om/build-all track-view (:tracks metadata) {:init-state {:player-chan player-chan}
                                                                                                   :state {:game_id game_id}}))}))))))
