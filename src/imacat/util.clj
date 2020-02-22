(ns imacat.util)

(defn fn->f1
  "Converts a clojure function into a Kotlin Function1"
  [function]
  (reify kotlin.jvm.functions.Function1
    (invoke [_ p1]
      (function p1))))

(defn fn->f2
  "Converts a clojure function into a Kotlin Function2"
  [function]
  (reify kotlin.jvm.functions.Function2
    (invoke [_ p1 p2]
      (function p1 p2))))
