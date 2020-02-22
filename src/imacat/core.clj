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
  (cond
    (or (= x (dec game-dimensions))
        (= x 0)
        (= y (dec game-dimensions))
        (= y 0))
    [:fence]
    :else [:grass]))

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
  {:name "GAME!!!!!"
   :world (world/gen-world game-dimensions gen-square-fn)})

(defn new-state
  "Create a brand new game state."
  ([]
   (let [state (agent {:game (atom (new-game))
                       ;;                :console (new-console)
                       ;;                :frame (new-frame)
                       :event-handler (atom nil)})]
     state)))

;;; Initial tile definitions
;; TODO: colors, etc.
(def tiles
  {:fence {:tile \f
           :fg-color org.hexworks.zircon.api.color.ANSITileColor/WHITE
           :bg-color org.hexworks.zircon.api.color.ANSITileColor/GRAY}
   :grass {:tile \.
           :fg-color org.hexworks.zircon.api.color.ANSITileColor/BRIGHT_GREEN
           :bg-color org.hexworks.zircon.api.color.ANSITileColor/GREEN}})

(defn print-game
  [game]
  (println (:name game))
  (doseq [x (range game-dimensions) y (range game-dimensions)]
    (let [{:keys [stuff]} (-> (:world game)
                              (get-in [x y]))]
      (print (str " " (get-in tiles [(first stuff) :tile])))
      ;; TODO: render characters
      (when (= y (dec game-dimensions))
        (println)))))

(def running true)

(defn main-handler
  [state]
  (when running
    (println (str "state: " state))
    (send-off *agent* main-handler)
    (Thread/sleep 2000)
    (when-let [game (:game state)]
      (print-game @game)
      ;; TODO: get input, change game state
      (println "printed game")
      (send *agent* assoc :k 1 :v (rand 100))
      (assoc state :k (keyword (rand 100)) :v (keyword (rand 100))))))

(defn start
  [state]
  (send-off state main-handler))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; GAME CONTEXT STUFF
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defonce state (new-state))

(defn handle-keypress
  [keypress]
  (println (str keypress))
  (cond
    (.equals keypress KeyCode/UP)
    (println "UP")
    (.equals keypress KeyCode/DOWN)
    (println "DOWN")
    (.equals keypress KeyCode/LEFT)
    (println "LEFT")
    (.equals keypress KeyCode/RIGHT)
    (println "RIGHT"))
;  (print-game @(:game @state))
  )

(defn get-tile-info
  [x y]
  (let [game @(:game @state)
        {:keys [stuff]} (-> (:world game)
                            (get-in [x y]))
        character (get-in tiles [(first stuff) :tile])
        bg-color (get-in tiles [(first stuff) :bg-color])
        fg-color (get-in tiles [(first stuff) :fg-color])]
    (as-> {} info
      (when bg-color
        (assoc info :bg-color bg-color))
      (when fg-color
        (assoc info :fg-color fg-color))
      (when character
        (assoc info :character character)))))

(defn init-runtime
  []
  (Thread/setDefaultUncaughtExceptionHandler
   (reify Thread$UncaughtExceptionHandler
     (uncaughtException [_ thread ex]
       (log/error ex "Uncaught exception on" (.getName thread))))))

(defn -main
  []
  (init-runtime)
  (ui/main-test handle-keypress get-tile-info))
;  (doto (new-state)
;    start))

;;;;;;;;;n;;;;;;;;;;;; graveyard ;;;;;;;;;;;;;;;;;;;;;;;;;
(comment
  ;;; copied launch code
  (defn launch) 
  "Launch the game with an initial game state. Can be called from REPL."
  ([]
   (launch (new-state)))
  ([state]
   (let [
         ;^JFrame frame (:frame state)
         jc (:console state)]
;     (setup-input jc state) 
;     (.add (.getContentPane frame) jc)
;     (.pack frame)
;     (.setVisible frame true)
     (main-handler state) 
     frame)))
