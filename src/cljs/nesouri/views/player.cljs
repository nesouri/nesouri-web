(ns nesouri.views.player
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [cljs.core.async :as async :refer [put! chan alts!]]
            [clojure.string :as string]
            [om.core :as om :include-macros true]
            [om-tools.dom :as dom :include-macros true]
            [om-tools.core :refer-macros [defcomponent]]
            [om-bootstrap.nav :as n]
            [om-bootstrap.button :as b]
            [om-bootstrap.random :as r]
            [nesouri.util :as util]
            [nesouri.gme :as gme]
            [om-sync.util :refer [edn-xhr]]))

(defn get-or-create-engine [owner]
  (or (om/get-state owner :engine)
      (when-let [payload (om/get-state owner :audio-data)]
        (let [audio-context (om/get-state owner :audio-context)
              sample-rate (.. audio-context -sampleRate)
              current-track (om/get-state owner :current-track)
              engine (gme/open-data payload sample-rate)]
          (om/set-state! owner :engine engine)
          (gme/stereo-depth! engine 0.7)
          (gme/track! engine (-  current-track 1))))))

(defn playback-loop [owner e]
  (when-let [engine (get-or-create-engine owner)]
    (let [audio-buffer (om/get-state owner :audio-buffer)
          audio-channel-count (.. e -outputBuffer -numberOfChannels)
          audio-buffer-channel-size (- (/ (gme/buffer-size audio-buffer) audio-channel-count) 1)
          audio-channels [(.. e -outputBuffer (getChannelData 0)) (.. e -outputBuffer (getChannelData 1))]]
      (gme/decode! engine audio-buffer)
      (dotimes [i audio-buffer-channel-size] ;; TODO: very crude rendering
        (dotimes [n audio-channel-count]
          (let [offset (* (+ i n) 4)
                value (gme/buffer-sample-at audio-buffer offset)]
            (aset (nth audio-channels n) i value)))))))

(defn playback-jump [owner target]
  (let [engine (om/get-state owner :engine)
        track-count (gme/track-count engine)
        current-track (om/get-state owner :current-track)
        target-track (target current-track)]
    (when (and (>= target-track 0) (< target-track track-count))
      (gme/track! engine (- target-track 1))
      (om/set-state! owner :current-track target-track))))

(defn playback-random [owner]
  (edn-xhr {:method :get
            :url "random-track"
            :on-complete #(put! (om/get-state owner :player-chan) [(:track %) (:game_id %)])}))

(defn load-track [owner track data]
  (let [f (:file data)
        url (str "nsf/" (.substring f 0 (- (.-length f) 3)) ".nsf")] ;; remove once server side supports 7z extraction
    (util/arr-xhr-get
     {:url url
      :on-complete (fn [payload]
                     (om/set-state! owner :current-authors (:persons data))
                     (om/set-state! owner :current-tracks (:tracks data))
                     (om/set-state! owner :current-game-id (:game_id data))
                     (om/set-state! owner :current-title (:title data))
                     (om/set-state! owner :audio-data payload)
                     (om/set-state! owner :current-track track)
                     (om/set-state! owner :engine nil))})))

(defn render-current-title [state]
  (:title (first
           (filter #(= (:current-track state) (:track %))
                   (:current-tracks state)))))

(defn render-current-game [state]
  (dom/a {:href (str "#/details/" (:current-game-id state))}
         (:current-title state)))

(defn render-current-authors [state]
  (interpose ", " (map #(dom/a {:href (str "#/artist/" (:person_id %))} (:name %)) (:current-authors state))))

(defcomponent player-view [app owner]
  (init-state [_]
    (let [buffer-size (* 1024 32)
          channels-in 2
          channels-out 2
          audio-context (js/AudioContext.)
          audio-node (.. audio-context (createScriptProcessor (/ buffer-size channels-out) channels-in channels-out))
          audio-buffer (gme/buffer buffer-size)]
      (set! (.. audio-node -onaudioprocess) #(playback-loop owner %)) ;; should bind in will-mount, same for audio context/node
      {:audio-context audio-context
       :audio-node audio-node
       :audio-buffer audio-buffer
       :audio-data nil
       :current-game-id 0
       :current-track 1
       :current-tracks '()
       :current-title ""
       :current-authors ""
       :engine nil}))
  (will-mount [_]
    (let [audio-context (om/get-state owner :audio-context)
          audio-node (om/get-state owner :audio-node)
          player-chan (om/get-state owner :player-chan)]
      (.. audio-node (connect (. audio-context -destination)))
      (go (loop []
            (let [[track game_id] (<! player-chan)]
              (edn-xhr {:method :get
                        :url (str "games/" game_id)
                        :on-complete (fn [m] (load-track owner track m))})
              (recur))))))
  (will-unmount [_]
    (.disconnect (om/get-state owner :audio-node))
    (.close (om/get-state owner :audio-context)))
  (render-state [_ state]
                (n/navbar {:fixed-bottom? true
                           :brand (dom/img {:src "nesouri.png"})}
                          (n/nav {:justified true}
                                 (dom/li
                                  (b/button-group {:style {:padding-top "11px"}}
                                                  (b/button {:on-click #(playback-jump owner dec)}
                                                            (r/glyphicon {:glyph "step-backward"}))
                                                  (b/button {}
                                                            (r/glyphicon {:glyph "play"}))
                                                  (b/button {:on-click #(playback-jump owner inc)}
                                                            (r/glyphicon {:glyph "step-forward"}))
                                                  (b/button {:on-click #(playback-random owner)}
                                                            (r/glyphicon {:glyph "random"}))))
                                 (if (:audio-data state)
                                   (dom/li { :style { :top "15px" :left "10px"}}
                                           (dom/span {:style {:color "#aaa"}} (str "[" (:current-track state) "/" (count (:current-tracks state)) "] "))
                                           (dom/span (render-current-title state))
                                           (dom/span {:style {:color "#aaa"}} " from ")
                                           (dom/span (render-current-game state))
                                           (if (not-empty (:current-authors state))
                                             (do
                                               [(dom/span {:style {:color "#aaa"}} " by ")
                                               (dom/span (render-current-authors state))]))))))))
