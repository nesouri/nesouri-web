(ns nesouri.gme
  (:import Gme))

(defn buffer [buffer-size]
  (Gme.PcmBuffer. buffer-size))

(defn buffer-size [buffer]
  (. buffer -size))

(defn buffer-sample-at [buffer offset]
  (.. buffer (get-sample offset)))

(defn open-data [payload sample-rate]
  (Gme.Engine. payload sample-rate))

(defn stereo-depth! [engine depth]
  (.. engine (set-stereo-depth depth))
  engine)

(defn track! [engine track]
  (.. engine (start-track track))
  engine)

(defn decode! [engine audio-buffer]
  (.. engine (decode audio-buffer))
  engine)

(defn track-count [engine]
  (.. engine (track-count)))
