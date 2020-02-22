(ns imacat.world)


;; TODO: add more stuff.. cats, items, etc.
(defrecord Cell
    [location])

(defn build-cell
  [args]
  (map->Cell (merge {:location [0 0]} args)))

(defn gen-world
  [dim populate-fn]
  (mapv (fn [x]
          (mapv (fn [y] (build-cell {:location [x y] :stuff (populate-fn x y)}))
                (range dim)))
        (range dim)))

