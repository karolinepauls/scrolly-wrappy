(ns scrolly-wrappy.core
  "A Reagent component to wrap an element, providing drag-to-scroll vertically synchronized with the
  browser window and horizontally scrolling its own viewport."
  (:require
    [reagent.core :as r]
    [goog.events :as events]))


(defn scrolly-wrappy [element-width initial-centre is-dragged-atom element]
  (let [drag-start-mouse-x (r/atom nil)
        drag-start-mouse-y (r/atom nil)
        drag-start-wrapper-scroll-x (r/atom nil)
        drag-start-window-scroll-y (r/atom nil)]

    (r/create-class
     {:display-name "scrolly-wrappy"

      :component-did-mount
      (fn scroll-sync [this]
        (let [scrollbar (aget this.refs "scrollbar-top")
              overflow-wrapper (aget this.refs "overflow-wrapper")
              visible-width overflow-wrapper.offsetWidth
              initial-left-edge-offset (- initial-centre (/ visible-width 2))
              apply-scroll (fn apply-scroll [element left-offset]
                             (aset element "scrollLeft" left-offset))
              copy-scroll (fn copy-scroll [destination source]
                            (apply-scroll destination source.target.scrollLeft))]

          ;; Mirror scrollbars position.
          (events/listen scrollbar "scroll" (partial copy-scroll overflow-wrapper))
          (events/listen overflow-wrapper "scroll" (partial copy-scroll scrollbar))

          ;; Apply initial offset.
          (apply-scroll overflow-wrapper initial-left-edge-offset)

          ;; Drag-to-scroll
          ;; We listen to mousemove and mouseup on window, so we get the events even if the cursor
          ;; is outside of the document.
          (let [apply-mouse-drag (fn [e]
                                   (let [x-delta (- @drag-start-mouse-x e.screenX)
                                         y-delta (- @drag-start-mouse-y e.screenY)
                                         horizontal-scroll-offset (+ @drag-start-window-scroll-y y-delta)
                                         veritcal-scroll-offset (+ @drag-start-wrapper-scroll-x x-delta)]
                                     (reset! is-dragged-atom true)
                                     (js/requestAnimationFrame
                                      (fn []
                                        (apply-scroll overflow-wrapper veritcal-scroll-offset)
                                        (.scrollTo js/window
                                                   js/window.scrollX horizontal-scroll-offset))))
                                   (doto e .preventDefault .stopPropagation))
                start-mouse-drag (fn [e]
                                   (when (= e.button 0)
                                     (reset! drag-start-mouse-x e.screenX)
                                     (reset! drag-start-mouse-y e.screenY)
                                     (reset! drag-start-wrapper-scroll-x overflow-wrapper.scrollLeft)
                                     (reset! drag-start-window-scroll-y js/window.scrollY)
                                     (events/listen js/window "mousemove" apply-mouse-drag)
                                     (doto e .preventDefault .stopPropagation)))
                stop-mouse-drag (fn [e]
                                  (when (= e.button 0)
                                    (events/unlisten js/window "mousemove" apply-mouse-drag)
                                    (reset! is-dragged-atom false)
                                    (doto e .preventDefault .stopPropagation)))]
            (events/listen overflow-wrapper "mousedown" start-mouse-drag)
            (events/listen js/window "mouseup" stop-mouse-drag))))

      :reagent-render
      (fn scrolly-wrappy-render [element-width _ _ element]
        [:div
         ;; Scrollbar on the top:
         [:div.scrollbar {:style {:overflow-x "auto" :overflow-y "hidden" :height "20px"}
                          :ref "scrollbar-top"}
          [:div {:style {:width element-width :height "20px"}}]]

         ;; Scroll wrapper with a scrollbar on the bottom:
         [:div.overflow-wrapper {:style {:overflow-x "auto"} :ref "overflow-wrapper"}
          [:div {:style {:width element-width :margin "0 auto"}}
           element]]])})))
