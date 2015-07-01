(ns nesouri.util
  (:require [goog.events :as events])
  (:import [goog.net XhrIo]
           goog.net.EventType
           [goog.events EventType]))

(defn arr-xhr-get [{:keys [url on-complete on-error]}]
  (let [xhr (XhrIo.)]
    (.. xhr (setResponseType goog.net.XhrIo.ResponseType.ARRAY_BUFFER))
    (events/listen xhr goog.net.EventType.SUCCESS
                   (fn [e] (on-complete (js/Uint8Array. (.. xhr (getResponse))))))
    (events/listen xhr goog.net.EventType.ERROR
                   (fn [e] (on-error {:error (.getResponseText xhr)})))
    (. xhr (send url "GET" #js {"Accept" "application/octet-stream"}))))
