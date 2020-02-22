(ns imacat.world)


;; TODO: add more stuff.. cats, items, etc.
(defrecord Cell
    [location])

;; this is bad now
(defn build-cell
  [args]
  (map->Cell (merge {[0 0] {}} args)))

(defn gen-world
  [dim populate-fn]
  (let [wld (atom {})]
    (doseq [x (range dim) y (range dim)]
      (swap! wld assoc [x y] (populate-fn x y)))
    @wld))
  
;  (mapv (fn [x]
;          (mapv (fn [y] (build-cell {[x y] (populate-fn x y)}))
;                (range dim)))
;        (range dim)))
