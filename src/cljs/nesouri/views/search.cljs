(ns nesouri.views.search
  (:require [om.core :as om :include-macros true]
            [om-tools.dom :as dom :include-macros true]
            [om-tools.core :refer-macros [defcomponentk]]
            [om-bootstrap.panel :as p]
            [om-bootstrap.input :as i]
            [om-bootstrap.grid :as g]
            [om-bootstrap.button :as b]
            [om-sync.util :refer [edn-xhr]]
            [om-tools.mixin :refer-macros [defmixin]]))

;; view-source:http://bootsnipp.com/iframe/94ypl

(defn filter-data
  [alist filter-text]
  (if-not (nil? filter-text)
    (let [pattern (js/RegExp filter-text "i")]
      (filter #(re-find pattern (:title %)) alist))
    alist))

(defcomponentk entry-view
  [[:data title game_id] state]
  (render [_]
          (dom/a {:className "list-group-item col-lg-3"
                  :href (str "#/details/" game_id)
                  :style {:white-space "nowrap"
                          :overflow "hidden"
                          :text-overflow "ellipsis"}}
                 title)))

(defmixin infinite-scroll-mixin
  (will-mount
   [owner]
   (set! (. owner -inf-scroll-func) #(.inf-scroll owner))
   (set! (. owner -inf-offset) 0))
  (did-mount
   [owner]
   (set! (. owner -inf-elem) (.getElementById js/document (. owner -inf-elem)))
   (.inf-loader owner #(.inf-scroll-resume owner owner) (.-inf-initial-size owner) 0))
  (will-unmount
   [owner]
   (.inf-scroll-suspend owner))
  (inf-scroll
   [owner ev]
   (let [elem (. owner -inf-elem)
         two-off-height (* 2 (.-offsetHeight elem))
         scr-height (.-scrollHeight elem)
         scr-top (.-scrollTop elem)]
     (if (>= (+ scr-top two-off-height 10) scr-height)
       (let [count (+ 1 (. owner -inf-offset))
             limit (. owner -inf-batch-size)
             offset (* count limit)]
         (.inf-scroll-suspend owner)
         (.inf-loader owner #(.inf-scroll-resume owner owner) limit offset)
         (set! (. owner -inf-offset) count)))))
  (inf-scroll-resume
   [owner]
   (.addEventListener (. owner -inf-elem) "scroll" (. owner -inf-scroll-func)))
  (inf-scroll-suspend
   [owner]
   (.removeEventListener (. owner -inf-elem) "scroll" (. owner -inf-scroll-func)))
  (inf-initial-size
   [owner initial-size]
   (set! (. owner -inf-initial-size) initial-size))
  (inf-batch-size
   [owner batch-size]
   (set! (. owner -inf-batch-size) batch-size))
  (inf-element
   [owner elem]
   (set! (. owner -inf-elem) elem))
  (inf-loader
   [owner loader]
   (set! (. owner -inf-loader) loader)))

(defcomponentk search-view-real
  [owner state]
  (:mixins infinite-scroll-mixin)
  (init-state
   [_]
   {:text nil
    :games '()
    :seconds 0})
  (will-mount
   [_]
   (.inf-initial-size owner 256)
   (.inf-batch-size owner 256)
   (.inf-element owner "result")
   (.inf-loader owner (fn [resume limit offset]
                        (edn-xhr {:method :get
                                  :url (str "games" "?limit=" limit "&offset=" offset)
                                  :data {:limit limit :offset offset}
                                  :on-complete #(swap! state update-in [:games] (fn [prev]
                                                                                  (resume owner)
                                                                                  (concat prev %)
                                                                                  ))}))))
  (render-state
   [_ {:keys [games text]}]
   (dom/div {:id "games" :className "container"}
            (dom/div { :className "row"}
                     (i/input {:type "text"
                               :value text
                               :placeholder "Search..."
                               :on-change #(swap! state update-in [:text] (fn [_] (.. % -target -value)))}))
            (dom/div {:id "result"
                      :className "row"
                      :style {:overflow-y "scroll"
                              :height "70%"}}
                     (om/build-all entry-view (filter-data games text))))))

(defcomponentk search-view
  []
  (render
   [_]
   (->search-view-real {}))) ;; how to do this outside?
