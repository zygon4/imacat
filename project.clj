(defproject imacat "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0"
            :url "https://www.eclipse.org/legal/epl-2.0/"}
  :dependencies [[org.clojure/clojure "1.10.0"]
                 [org.hexworks.zircon/zircon.core-jvm "2020.0.1-PREVIEW"]
                 [org.hexworks.zircon/zircon.jvm.swing "2020.0.1-PREVIEW"]
                 [org.jetbrains.kotlinx/kotlinx-collections-immutable-jvm "0.3"]
                 [org.clojure/tools.logging "0.6.0"]]
  :repositories [["jitpack.io" "https://jitpack.io"]
                 ["KotlinX" "https://dl.bintray.com/kotlin/kotlinx/"]]
  :repl-options {:init-ns imacat.core}

  :main imacat.core)

