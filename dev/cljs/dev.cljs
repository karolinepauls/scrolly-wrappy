(ns scrolly-wrappy.dev
  "Example code using scrolly-wrappy. Run `lein figwheel` to see."
  (:require
    [reagent.core :as r]
    [scrolly-wrappy.core :refer [scrolly-wrappy]]))

(def svg-size (r/atom 2000))

(defonce selected-demo (r/atom :svg))
(defonce table-size (r/atom 40))

(def half (partial * 0.5))

(defn scale [size margin n]
  (+ (* (half size) n) (half size) margin))

(defn lissajous-seq [margin width height delta a b steps-count]
  (let [full-circle (* 2 js/Math.PI)
        step (/ full-circle steps-count)
        epsilon (* step 0.1)
        up-to (+ full-circle epsilon)
        inner-width (- width (* 2 margin))
        inner-height (- height (* 2 margin))
        scale-w (partial scale inner-width margin)
        scale-h (partial scale inner-height margin)]
    (for [t (range 0 full-circle step)]
      [(scale-w (js/Math.sin (+ (* a t) delta)))
       (scale-h (js/Math.sin (* b t)))])))

(defn demo-svg [margin width height]
  (let [[[x-start y-start] seq-rest]
        ((juxt first rest)
         (lissajous-seq margin width height
                        0
                        5 6
                        1000))]
    [:svg.sample {:width width :height height :viewBox (str "0 0 " width " " height)}
     [:path {:d (apply str
                       (cons
                         (str "M " x-start " " y-start)
                         (for [[x y] seq-rest]
                           (str " L " x " " y))))
             :fill "none"
             :stroke "#231"
             :stroke-width "2px"}]
     ]))

(defn demo-table [n]
  (let [num-range (range 1 (inc n))
        multiplication-table (partition n (for [x num-range y num-range] (* x y)))]
    [:table.sample
     [:tbody
      (for [row multiplication-table]
        [:tr {:key (first row)}
         (for [cell row] [:td {:key cell} cell])
         ])]]))

(defn demo-view []
  [:div.demo
   (case @selected-demo
     :svg ^{:key :svg} [scrolly-wrappy
                        [demo-svg 5 @svg-size (half @svg-size)]]
     :table ^{:key :table} [scrolly-wrappy
                            {:initial-centre-fn (constantly 0)}
                            [demo-table @table-size]])])

;; Written for README.
;(def is-dragged? (r/atom false))
;
;(defn demo-view []
;  [scrolly-wrappy {:initial-centre-fn (fn [width] (- width (/ width 4)))
;                   :on-drag-start #(reset! is-dragged? true)
;                   :on-drag-end #(reset! is-dragged? false)}
;   [:svg {:width "2000px" :height "2000px"}
;    [:path {:d "M 1 1999 L 999 1 L 1999 1999 L 1 1999" :fill "#aaa" :stroke "#333"}]]])

(defn page []
  [:div
   [:header
    [:h1 "Scrolly-wrappy demo"]

    [:form.controls
     [:label {:for "demo"} "Demo "]
     [:select {:id "demo"
               :value (name @selected-demo)
               :on-change #(reset! selected-demo (-> % .-target .-value keyword))}
      [:option {:value "svg"} "SVG"]
      [:option {:value "table"} "Table"]]

     (case @selected-demo
       :table [:span
               [:label {:for "table-size"} "Table size "]
               [:select {:id "table-size"
                         :value @table-size
                         :on-change #(reset! table-size (-> % .-target .-value js/parseInt))}
                (for [n [10 20 30 40 50]]
                  [:option {:value n :key n} n])]]
       :svg [:span
               [:label {:for "svg-size"} "SVG size "]
               [:select {:id "svg-size"
                         :value @svg-size
                         :on-change #(reset! svg-size (-> % .-target .-value js/parseInt))}
                (for [n [200 500 1000 1500 2000 4000 8000 16000]]
                  [:option {:value n :key n} n])]])]

    [:ul.links
     [:li [:a {:href "https://gitlab.com/karolinepauls/scrolly-wrappy"} "Repo"]]
     [:li [:a {:href "https://clojars.org/scrolly-wrappy"} "Package"]]]]
   [demo-view]
   [:footer
    [:p
     "Special thanks to: Clojure/ClojureScript community, the Reagent project, "
     "and Infectious Media, who opened the codebase this module got extracted from."]]])

(defn init! []
  (r/render [page] (.getElementById js/document "app")))

(init!)

(defn on-js-reload []
  (println "Reloaded."))
