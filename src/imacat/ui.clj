(ns imacat.ui
  (:require [imacat.util :as util])
  (:import (org.hexworks.zircon.api Components
                                    ComponentDecorations
                                    CP437TilesetResources
                                    SwingApplications)
           (org.hexworks.zircon.api.application AppConfig)
           (org.hexworks.zircon.api.color ANSITileColor)
           (org.hexworks.zircon.api.data Position Size Tile)
           (org.hexworks.zircon.api.graphics BoxType)
           (org.hexworks.zircon.api.screen Screen)
           (org.hexworks.zircon.api.uievent ComponentEventType
                                            KeyboardEventType
                                            UIEventResponse)
           (java.awt Toolkit)))


(defonce tileset (CP437TilesetResources/rexPaint16x16))

(defn position
  [x y]
  (Position/create x y))

(defn size
  [width height]
  (Size/create width height))

(defn get-grid-size
  []
  (let [screen-size (.getScreenSize (Toolkit/getDefaultToolkit))
        columns (/ (.getWidth screen-size) (.getWidth tileset))
        rows (/ (.getHeight screen-size) (.getHeight tileset))]
    (size columns rows)))

(defn create-button
  [text position]
  (-> (Components/button)
      (.withText text)
      (.withPosition position)
      (.build)))

(defn create-panel
  [position size]
  (-> (Components/panel)
;      (.withDecorations (ComponentDecorations/box BoxType/LEFT_RIGHT_DOUBLE))
      (.withPosition position)
      (.withSize size)
      (.build)))

(defn create-tile
  [bg-color fg-color character]
  (-> (Tile/newBuilder)
      (.withBackgroundColor bg-color)
      (.withForegroundColor fg-color)
      (.withCharacter character)
      (.buildCharacterTile)))

(defn app-config
  [size]
  (-> (AppConfig/newBuilder)
      (.withSize size)
      (.withDefaultTileset tileset)
;      (.fullScreen)
      (.build)))

;; These defaults are a little weird, need to default the empty
;; key, and default if the key is nil
(defn print-tile
  [tile-grid get-tile-info-fn x y]
  (try
    (let [{:keys [bg-color fg-color character]
           :or {bg-color org.hexworks.zircon.api.color.ANSITileColor/BLACK
                fg-color org.hexworks.zircon.api.color.ANSITileColor/BLACK
                character \~}
           :as tile-info} (get-tile-info-fn x y)
          t (create-tile bg-color fg-color character)
          pos (position x y)]
      (.draw tile-grid t pos))
  (catch Exception e
    (.printStackTrace e))))

(defn print-panel
  "This is pretty heavyweight printing of everything,
  should not be called often."
  [tile-grid get-tile-info-fn x-dim y-dim]
  (try
    (doseq [x (range x-dim) y (range y-dim)]
      (print-tile tile-grid get-tile-info-fn x y))
    (catch Exception e
      (.printStackTrace e))))

(defn main-test
  [keypress-fn get-tile-info-fn]
  "Assumes that keypress-fn returns a seq of tile coordinates (or nil) to refresh via get-tile-info-fn."
  (let [config
        ;; Get grid size is a little wonky now, just use 80/60
        (app-config (size 80 60))
        tile-grid (SwingApplications/startTileGrid config)
        main-menu-screen (Screen/create tile-grid)
        main-panel (create-panel (position 0 0) (size 15 15))
        quit-button (create-button "quit" (position 0 0))
        quit-fn (util/fn->f1
                 (fn [event]
                   (System/exit 0)
                   (UIEventResponse/processed)))
        key-fn (util/fn->f2
                (fn [event phase]
                  (do
                    (println (str "got " phase " " (.getCode event)))
                    (when-let [tile-updates (keypress-fn (.getCode event))]
                      (doseq [[x y] tile-updates]
                        (print-tile tile-grid get-tile-info-fn x y))))))]
    (.addComponent main-panel quit-button)
    (.processKeyboardEvents tile-grid KeyboardEventType/KEY_PRESSED key-fn)
    (.handleComponentEvents quit-button ComponentEventType/ACTIVATED quit-fn)
    (.display main-menu-screen)
    (print-panel tile-grid get-tile-info-fn 80 60)))


    
