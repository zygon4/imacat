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

;; Calls back into the non-ui code to get tile info
;; org.hexworks.zircon.api.color TileColor)

;; These defaults are a little weird, need to default the empty
;; key, and default if the key is nil
(defn print-panel
  [tile-grid get-tile-info-fn x-dim y-dim]
  (try
    (doseq [x (range x-dim) y (range y-dim)]
      (let [{:keys [bg-color fg-color character]
             :or {bg-color org.hexworks.zircon.api.color.ANSITileColor/BLACK
                  fg-color org.hexworks.zircon.api.color.ANSITileColor/BLACK
                  character \~}
             :as tile-info} (get-tile-info-fn x y)
            c (or character \~)
            t (create-tile bg-color
                           (or fg-color
                               org.hexworks.zircon.api.color.ANSITileColor/BLACK)
                           c)
            pos (position x y)]
        (.draw tile-grid t pos)))
    (catch Exception e
      (.printStackTrace e))))

(defn print-panel-1
  [tile-grid get-tile-info-fn x-dim y-dim]
  (doseq [x (range x-dim) y (range y-dim)]
    (let [{:keys [bg-color fg-color character]
           :or {bg-color org.hexworks.zircon.api.color.ANSITileColor/BLACK
                fg-color org.hexworks.zircon.api.color.ANSITileColor/BLACK
                character \s}} (get-tile-info-fn x y)
          t (create-tile bg-color fg-color character)
          pos (position x y)]
      (do
        (println (str pos ") bg " bg-color " fg " fg-color " character " character))
        (println t)))))
;        (.draw tile-grid
;               (create-tile bg-color fg-color character)
;               (position x y))))))

(defn main-test
  [keypress-fn get-tile-info-fn]
  (let [config
                                        ;(app-config (get-grid-size))
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
;                    (keypress-fn (.getCode event))
                    (print-panel tile-grid get-tile-info-fn 80 60)))) 
        ;; TBD: Print world here? How? Does the UI understand the game state or
        ;; does the game understand UI concepts/API?
        ]
    (.addComponent main-panel quit-button)
    (.processKeyboardEvents tile-grid KeyboardEventType/KEY_PRESSED key-fn)
    (.handleComponentEvents quit-button ComponentEventType/ACTIVATED quit-fn)
    (.display main-menu-screen)
    (print-panel tile-grid get-tile-info-fn 80 60)))


    
