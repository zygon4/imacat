(ns imacat.tbd)

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

