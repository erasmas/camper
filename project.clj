(defproject camper "0.0.1-SNAPSHOT"
  :description "Download manager for your music library on Bandcamp"
  :license {:name "Attribution 4.0 International (CC BY 4.0)"
            :url "https://creativecommons.org/licenses/by/4.0/"}
  :dependencies [[org.clojure/clojure "1.10.3"]
                 [etaoin/etaoin "0.4.6"]
                 [org.clojure/tools.logging "1.2.4"]
                 [org.clojure/tools.cli "1.0.206"]]
  :plugins [[cider/cider-nrepl "0.24.0"]]
  :main ^:skip-aot camper.core
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all
                       :jvm-opts ["-Dclojure.compiler.direct-linking=true"]}})
