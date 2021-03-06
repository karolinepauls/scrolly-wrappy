(ns scrolly-wrappy.core
  "A Reagent component to wrap an element, providing drag-to-scroll vertically synchronized with the
  browser window and horizontally scrolling its own viewport."
  (:require
    [reagent.core :as r]
    [goog.events :as events]
    [goog.object :as o]))

(def ^{:private true} start-drag-events ["mousedown" "touchstart"])
(def ^{:private true} stop-drag-events ["mouseup" "touchend" "touchcancel"])
(def ^{:private true} drag-events ["mousemove" "touchmove"])

(defn- noop [])

(defn scrolly-wrappy
  "Wrap content horizontally and enable horizontal and vertical drag-to-scroll. Vertical scroll is
  window scroll.

  Options:
  initial-centre-fn takes wrapped element width in px and returns which pixel should be centred.
  The default centres in half.
  on-drag-start on-drag-end are user callbacks with no args, useful to coordinate."
  ([element] (scrolly-wrappy {} element))
  ([{:keys [initial-centre-fn on-drag-start on-drag-end]
     :or {on-drag-start noop
          on-drag-end noop
          initial-centre-fn (fn [width] (/ width 2))}}
    element]
   (let [drag-start-mouse-x (atom nil)
         drag-start-mouse-y (atom nil)
         drag-start-wrapper-scroll-x (atom nil)
         drag-start-window-scroll-y (atom nil)]

     (r/create-class
       {:display-name "scrolly-wrappy"

        :component-did-mount
        (fn setup-scroll-sync [this]
          (let [scrollbar (o/get this.refs "scrollbar-top")
                overflow-wrapper (o/get this.refs "overflow-wrapper")
                wrapped-dom (aget (.-childNodes overflow-wrapper) 0)
                wrapped-width (.-width (js/getComputedStyle wrapped-dom))
                top-scrollbar-width-box (o/get this.refs "top-scrollbar-width-box")
                initial-centre (initial-centre-fn (js/parseInt wrapped-width))
                visible-width overflow-wrapper.offsetWidth
                initial-left-edge-offset (- initial-centre (/ visible-width 2))
                apply-scroll (fn apply-scroll [element left-offset]
                               (set! (.-scrollLeft element) left-offset))
                copy-scroll (fn copy-scroll [destination source]
                              (apply-scroll destination source.target.scrollLeft)
                              (doto source .preventDefault .stopPropagation))]

            ;; Apply wrapped DOM width to the top scrollbar.
            (set! (.. top-scrollbar-width-box -style -width) wrapped-width)

            ;; Mirror scrollbars position.
            (events/listen scrollbar "scroll" (partial copy-scroll overflow-wrapper))
            (events/listen overflow-wrapper "scroll" (partial copy-scroll scrollbar))

            ;; Apply initial offset.
            (apply-scroll overflow-wrapper initial-left-edge-offset)

            ;; Drag-to-scroll
            ;; We listen to mousemove and mouseup on window, so we get the events even if the cursor
            ;; is outside of the document.
            (letfn [(apply-mouse-drag [e]
                      (let [x-delta (- @drag-start-mouse-x e.screenX)
                            y-delta (- @drag-start-mouse-y e.screenY)
                            horizontal-scroll-offset (+ @drag-start-window-scroll-y y-delta)
                            veritcal-scroll-offset (+ @drag-start-wrapper-scroll-x x-delta)]
                        (on-drag-start)
                        (js/requestAnimationFrame
                          (fn []
                            (apply-scroll overflow-wrapper veritcal-scroll-offset)
                            (.scrollTo js/window
                                       js/window.scrollX horizontal-scroll-offset))))
                      (doto e .preventDefault .stopPropagation))
                    (start-drag [e]
                      (when (or (and (= e.type "mousedown") (= e.button 0))
                                (= e.type "touchstart"))
                        (reset! drag-start-mouse-x e.screenX)
                        (reset! drag-start-mouse-y e.screenY)
                        (reset! drag-start-wrapper-scroll-x overflow-wrapper.scrollLeft)
                        (reset! drag-start-window-scroll-y js/window.scrollY)

                        (doseq [ev drag-events]
                          (events/listen js/window ev apply-mouse-drag))
                        (doseq [ev stop-drag-events]
                          (events/listen js/window ev stop-drag))

                        (doto e .preventDefault .stopPropagation)))
                    (stop-drag [e]
                      (when (or (and (= e.type "mouseup") (= e.button 0))
                                (= e.type "touchend")
                                (= e.type "touchcancel"))
                        (doseq [ev drag-events]
                          (events/unlisten js/window ev apply-mouse-drag))
                        (doseq [ev stop-drag-events]
                          (events/unlisten js/window ev stop-drag))

                        (on-drag-end)
                        (doto e .preventDefault .stopPropagation)))]
              (doseq [ev start-drag-events]
                (events/listen overflow-wrapper ev start-drag)))))

        :component-did-update
        (fn update-width [this]
          (let [overflow-wrapper (o/get this.refs "overflow-wrapper")
                wrapped-dom (aget (.-childNodes overflow-wrapper) 0)
                wrapped-width (.-width (js/getComputedStyle wrapped-dom))
                top-scrollbar-width-box (o/get this.refs "top-scrollbar-width-box")]
            (set! (.. top-scrollbar-width-box -style -width) wrapped-width)))

        :reagent-render
        (fn scrolly-wrappy-render
          ([_ element] (scrolly-wrappy-render element))
          ([element]
           [:div.scrolly-wrappy
            ;; Top scrollbar:
            [:div.scrolly-wrappy-top-scrollbar
             {:style {:overflow-x "auto"} :ref "scrollbar-top"}
             ;; Fake content to force the scrollbar to appear. Must have some height, so it affects
             ;; the elements. Width should be dynamically set to wrapped content width.
             [:div {:ref "top-scrollbar-width-box" :style {:height "1px" :visibility "hidden"}}]]

            ;; Scroll wrapper with a scrollbar on the bottom:
            [:div.scrolly-wrappy-wrapper {:style {:overflow-x "auto"} :ref "overflow-wrapper"}
             element]]))}))))
