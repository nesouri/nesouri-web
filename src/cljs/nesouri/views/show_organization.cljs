(ns nesouri.views.show-organization
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [cljs.core.async :as async :refer [put! chan alts!]]
            [om.core :as om :include-macros true]
            [om-tools.dom :as dom :include-macros true]
            [om-tools.core :refer-macros [defcomponent]]
            [om-bootstrap.grid :as g]
            [om-bootstrap.panel :as p]
            [om-bootstrap.table :refer [table]]
            [om-sync.util :refer [edn-xhr]]))

(defcomponent game-view [game owner]
  (render-state [_ state]
                (dom/tr
                 (dom/td (dom/a {:href (str "#/details/" (:game_id game))} (:title game)))
                 (dom/td (or (:date game) (:year game)))
                 (dom/td (dom/a {:href (str "#/organizations/" ((:org-id state) game))} ((:org state) game))))))

(defcomponent show-organization [app owner]
  (will-mount [_]
              (edn-xhr {:method :get
                        :url (str "organizations/" (om/get-state owner :organization_id))
                        :on-complete #(om/set-state! owner :metadata %)}))
  (render-state [_ {:keys [metadata]}]
                (g/grid {:style { :bottom "30px"}}
                        (g/row {:class "show-grid"}
                               (g/col {:lg 1}
                                      (dom/a {:href "javascript:history.back()"} "Back"))
                               (g/col {:lg 4}
                                      (dom/h1 (:name metadata))))
                        (when-not (empty? (:releases metadata))
                          (g/row {:class "show-grid"}
                                 (g/col {:lg 10 :lg-offset 1}
                                        (dom/h4 "Releases:")
                                        (p/panel {:list-group
                                                  (table {:bordered? true :condensed? false :hover? true}
                                                         (apply dom/tbody
                                                                (om/build-all game-view (:releases metadata) {:init-state {:org :publisher
                                                                                                                           :org-id :publisher_id}})))}))))
                        (when-not (empty? (:publications metadata))
                          (g/row {:class "show-grid"}
                                 (g/col {:lg 10 :lg-offset 1}
                                        (dom/h4 "Publications:")
                                        (p/panel {:list-group
                                                  (table {:bordered? true :condensed? false :hover? true}
                                                         (apply dom/tbody
                                                                (om/build-all game-view (:publications metadata) {:init-state {:org :developer
                                                                                                                               :org-id :developer_id}})))})))))))
