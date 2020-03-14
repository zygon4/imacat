(ns imacat.world)

(defn gen-world
  [dim populate-fn]
  (let [wld (atom {})]
    (doseq [x (range dim) y (range dim)]
      (swap! wld assoc [x y] (populate-fn x y)))
    @wld))
