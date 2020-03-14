(ns imacat.core
  (:require [imacat.world :as world]
            [imacat.ui :as ui]
            [clojure.tools.logging :as log])
  (:import (org.hexworks.zircon.api.uievent KeyCode))
  (:gen-class))

;;
;; Turn-based cat “sim”. You move around different environments:
;; House, yard, alley, wooded
;;Trying to stay alive.  You work in different modes:
;;
;;Prowl, charm, normal (default)
;;Along with stats:
;;Strength, perception, acrobatics, coat
;;And traits:
;;Aggression, spite, appetite
;;Plus fluctuating levels of:
;;Spite, hunger
;;

(def modes
  {:prowl "murder kitty"
   :play "happy kitty"
   :charm "sexy kitty"
   :default "hate you kitty"})

(def status
  {:hunger "feedmenow"
   :vision "iseeyou"
   :warmth "ihugit"
   :energy "inomit"})

;; TODO: stats e.g. str, dex, etc.

(defn birth-kitty
  []
  {:status {:hunger 1.0, :vision 1.0, :warmth 1.0, :energy 1.0}})

(defn- update-kitty-status
  [kitty status value-fn]
  (let [current-status-value (get-in kitty [:status status])
        new-status-value (value-fn current-status-value)]
    (assoc-in kitty [:status status] new-status-value)))

;; need to define HOW the change-rates are calculated
;; e.g. warmth change is a function of external temp
;;      relative to the kitty

(defn gen-environment
  []
  ;; TODO: the rest
  {:heat (* 1.0 (rand-int 10))})

;; min 0, max 10
(defn- norm-status
  [status-val]
  (Math/min 10.0 (Math/max 0.0 status-val)))

(defn- update-kitty-warmth
  [{:keys [heat] :or {heat 1}}]
  (fn [v]
    (let [heat-diff (- heat v)
          warmer (pos? heat-diff)
          ;; very naive fast walk towards the heat level
          abs-val (Math/sqrt (Math/abs heat-diff))]
      (if warmer (norm-status (+ v abs-val)) (norm-status (- v abs-val))))))

;; final public API for updating a kitty
(defn update-kitty
  [kitty env]
  (let [warmth-update
        (update-kitty-status kitty :warmth (update-kitty-warmth env))]
    ;; TODO: remaining env updates
    warmth-update))


;; TODO: :light affects vision


;; need to define a mechanism for "time elapse"
(defn calc-status-change
  [elapsed-secs change-rate]
  (* elapsed-secs change-rate))

;; This below is the original data shape, may hold water later, but it's also
;; a code document of some of the options
(comment (def stats)
  {:mode {:descriptions [:normal :charm :prowl]
          :meta-not-sure nil}
   :stats {:descriptions [:strength :perception :acrobatics :coat]}
           
   :traits {:descriptions [:aggression :appetite]}
            
   :status {:descriptions [:spite :hunger]}})

(def environment
  {:house
   {:size :medium
    :layout [:kitchen :living :hall] ;; this is plenty
    :locations {:kitchen [{:id "k1" :description "This is a big kitchen room"}]
                :living []
                :hall []}
    :items {:kitchen {:toy 10}}}})    





;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Running game code
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defonce game-dimensions 59)

(defn add-terrain [x y]
  (let [max-x (dec game-dimensions)
        max-y (dec game-dimensions)]
    (cond
      (or (= x max-x)
          (= x 0)
          (= y max-y)
          (= y 0))
      [:fence]
      (and (= x 1) (= y 1))
      [:grass :cat]
      :else [:grass])))

;; just a data structure to hold all the game context stuff,
;; probably needs to be broken out
(defn gen-square-fn
  ;; This needs more context to be useful, e.g. the surrounding
  ;; squares. Also note: they're "squares" here because we don't
  ;; need to know the underlying representation.
  [x y]
  (add-terrain x y))

(defn new-game
  []
  {:name "I'm A Cat!"
   :world (world/gen-world game-dimensions gen-square-fn)
   :player-location [1 1]})

;;; Initial tile definitions
(def tiles
  {:cat {:tile \@
         :fg-color org.hexworks.zircon.api.color.ANSITileColor/BLACK
         :bg-color org.hexworks.zircon.api.color.ANSITileColor/GREEN}
   :fence {:tile \f
           :fg-color org.hexworks.zircon.api.color.ANSITileColor/WHITE
           :bg-color org.hexworks.zircon.api.color.ANSITileColor/GRAY
           :flags {:impassable true}}
   :grass {:tile \.
           :fg-color org.hexworks.zircon.api.color.ANSITileColor/BRIGHT_GREEN
           :bg-color org.hexworks.zircon.api.color.ANSITileColor/GREEN}})

(defn print-game
  [game]
  (println (:name game))
  (doseq [x (range game-dimensions) y (range game-dimensions)]
    (let [stuff (get-in game [:world [x y]])]
      (print (str " " (get-in tiles [(first stuff) :tile])))
      ;; TODO: render characters
      (when (= y (dec game-dimensions))
        (println)))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; GAME CONTEXT STUFF
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;; game -> world
(defonce game (atom (new-game)))

(defn get-game
  "Returns the game data"
  []
  @game)

(defn get-world
  "Returns the world data from the game"
  ([]
   (:world (get-game)))
  ([game]
   (:world game)))

(defn get-player-location
  ([{:keys [player-location]}]
   player-location)
  ([]
   (get-player-location (get-game))))


(defn is-cat?
  [v]
  (= v :cat))

(defn is-impassable?
  "Returns true if this item has the :impassable flag set to true."
  [location-item]
  (let [tile (tiles location-item)]
    (or
     (and (contains? tile :flags)
          ((tile :flags) :impassable))
     false)))

(defn can-move?
  ([to-location from-location]
   (try
     (let [{:keys [world player-location]
            :as game} (get-game)
           to-loc (world to-location)
           from-loc (world from-location)]
       ;; FROM doesn't matter as much yet, eventually
       ;; need to have traps/grabs, reasons to be stuck
       ;; also shouldn't be necessarily only the player moving
;;       (println (str "Can move from " from-location " to " to-location))
;;       (println (str "Can move from " from-loc " to " to-loc))
;;       (println (str "can move? " (empty? (filter is-impassable? to-loc))))
       (empty? (filter is-impassable? to-loc)))
   (catch Exception e
     (.printStackTrace e))))
  ([to-location]
   (try
     (let [{:keys [player-location]} (get-game)]
       (println "player location " player-location)
       (can-move? to-location player-location))
  (catch Exception e
    (.printStackTrace e)))))

(defn handle-direction-keypress
  [keypress]
  (let [location-change
        (cond
          (.equals keypress KeyCode/UP)
          [0 -1]
          (.equals keypress KeyCode/DOWN)
          [0 +1]
          (.equals keypress KeyCode/LEFT)
          [-1 0]
          (.equals keypress KeyCode/RIGHT)
          [1 0])
        from-location (get-player-location)
        to-location (into []
                          (map + from-location location-change))]
    (when (can-move? to-location)
      (println (str "move to " to-location))
      (swap! game update-in [:world to-location]
             (fn [loc] (conj loc :cat)))
      (swap! game update-in [:world from-location]
             (fn [loc] (into [] (remove is-cat? loc))))
      (swap! game assoc :player-location to-location)
      nil)))
  

;; TODO: context aware key-press, this is only
;; assuming movement, no menus, etc.
(defn handle-keypress
  [keypress]
  (println (str keypress))
  (cond
    (or (.equals keypress KeyCode/UP)
        (.equals keypress KeyCode/DOWN)
        (.equals keypress KeyCode/LEFT)
        (.equals keypress KeyCode/RIGHT))
    (handle-direction-keypress keypress)))

(defn get-tile-info
  [x y]
  (try
    (let [stuff (get-in (get-game) [:world [x y]])
          character (get-in tiles [(first (sort (fn [a b] (if (= a :cat) -1 1)) stuff)) :tile])
          bg-color (get-in tiles [(first stuff) :bg-color])
          fg-color (get-in tiles [(first stuff) :fg-color])
          flags (get-in tiles [(first stuff) :flags])]
      (as-> {} info
        (when flags
          (assoc info :flags flags))
        (when bg-color
          (assoc info :bg-color bg-color))
        (when fg-color
          (assoc info :fg-color fg-color))
        (when character
          (assoc info :character character))))
    (catch Exception e
      (.printStackTrace e))))

(defn init-runtime
  []
  (Thread/setDefaultUncaughtExceptionHandler
   (reify Thread$UncaughtExceptionHandler
     (uncaughtException [_ thread ex]
       (.printStackTrace ex)
       (log/error ex "Uncaught exception on" (.getName thread))))))

(defn -main
  []
  (init-runtime)
  (ui/main-test handle-keypress get-tile-info))
