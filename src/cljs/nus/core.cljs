(ns nus.core
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [goog.events :as events]
            [cljs.core.async :refer [put! <! >! chan timeout]]
            [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [cljs-http.client :as http]))

(enable-console-print!)

(defn fetch-widgets
  [url]
  (let [c (chan)]
    (go (let [{widgets :body} (<! (http/get url))]
          (>! c (vec widgets))))
    c))

(def app-state
  (atom {:widgets  []}))

(defn handle-change [e news-item owner]
  (om/transact! news-item [:post/count] inc))

(defn parse-news-item [news-item-str]
  {:title news-item-str :author "Pysio" :count 0})


(defn add-news-item [app owner]
  (let [new-news-item (-> (om/get-node owner "new-news-item")
                          .-value
                          parse-news-item)]
    (when new-news-item
      (om/transact! app :widgets #(conj % new-news-item)))))

(defn news-item-view [news-item owner]
  (reify
    om/IRenderState
    (render-state [this {:keys [star]}]
                  (dom/tr nil
                          (dom/td #js {:width "50"}
                                  (dom/button #js {:className "tiny success button fi-star"
                                                   :onClick #(handle-change % news-item owner)} nil))
                          (dom/td #js {:width "50" :className "count"}
                                  (dom/span #js {:className "secondary label"} (:post/count news-item)))
                          (dom/td #js {:className "title" } (:post/title news-item))
                          (dom/td #js {:width "150"} (:person/name (:post/person news-item)))))))

(defn news-list-view [app owner opts]
  (reify
    om/IInitState
    (init-state [_]
                {:star (chan)})
    om/IWillMount
    (will-mount [_]
                (let [star (om/get-state owner :star)]
                  (go (loop []
                        (let [news-item (<! star)]
                          (.log js/console (pr-str news-item))
                          (recur))))))
;;                 (go (while true
;;                       (let [widgets (<! (fetch-widgets (:url opts)))]
;;                         (om/update! app [:widgets] widgets))
;;                       (<! (timeout (:poll-interval opts))))))
    om/IRenderState
    (render-state [this {:keys [star]}]
                  (dom/div nil
                           (dom/h2 nil "News")
                           (dom/input #js {:type "text" :ref "new-news-item"})
                           (dom/button #js {:onClick #(add-news-item app owner)
                                            :className "small button"} "Add")
                           (dom/hr nil)
                           (apply dom/table nil
                                  (om/build-all news-item-view (sort-by :count > (:widgets app))
                                                {:init-state {:star star}}))))))


(defn om-app [app owner]
  (reify
    om/IRender
    (render [_]
            (dom/div nil
                     (om/build news-list-view app
                               {:opts {:url "/widgets"
                                       :poll-interval 10000}})))))

(om/root om-app app-state
         {:target (. js/document (getElementById "content"))})

