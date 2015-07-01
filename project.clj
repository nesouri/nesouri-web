(defproject nesouri-web "0.1.0-SNAPSHOT"
  :description "Nesouri - A Nintendo Music player for the Internets"
  :url "https://github.com/nesouri"
  :min-lein-version "2.0.0"
  :dependencies [[org.clojure/clojure "1.7.0-RC1"]
                 [org.clojure/clojurescript "0.0-3308"]
                 [org.clojure/core.async "0.1.346.0-17112a-alpha"]

                 [org.clojure/tools.logging "0.3.1"]
                 [org.slf4j/slf4j-log4j12 "1.7.12"]
                 [log4j/log4j "1.2.17" :exclusions [javax.mail/mail
                                                    javax.jms/jms
                                                    com.sun.jmdk/jmxtools
                                                    com.sun.jmx/jmxri]]
                 [com.cemerick/url "0.1.1"]

                 [org.omcljs/om "0.8.8"]
                 [prismatic/om-tools "0.3.11"]
                 [secretary "1.2.3"]
                 [om-syncing "0.1.2"]
                 [racehub/om-bootstrap "0.5.1"]
                 [garden "1.2.6"]

                 [ring "1.4.0-RC1"]
                 [compojure "1.3.4"]
                 [ring/ring-defaults "0.1.5"]

                 [sqlitejdbc "0.5.6"]
                 [korma "0.4.2"]
                 [cheshire "5.5.0"]
                 [blackwater "0.0.9"]]

  :plugins [[lein-ancient "0.6.7"]
            [lein-cljsbuild "1.0.6"]
            [lein-figwheel "0.3.3"]]

  :source-paths ["src/clj" "src/cljs"]
  :resource-paths ["resources"]

  :figwheel {:ring-handler nesouri.core/app}
  :cljsbuild
  {:builds
   {:dev {:figwheel true
          :source-paths ["src/clj" "src/cljs"]
          :compiler {:output-to "resources/public/js/app.js"
                     :output-dir "resources/public/js/app"
                     :main nesouri.core
                     :asset-path "js/app"
                     :optimizations :none
                     :source-map true
                     :source-map-timestamp true
                     :foreign-libs [{:file "src/js/gme/gme.js"
                                     :file-min "src/js/gme/gme.js"
                                     :provides ["Gme"]}]
                     :externs ["src/js/gme/gme-externs.js"]}}
    :prod {:figwheel {}
           :source-paths ["src/clj" "src/cljs"]
           :compiler {:output-to "resources/public/js/app.min.js"
                      :output-dir "resources/public/js/app-prod"
                      :optimizations :advanced
                      :elide-asserts true
                      :pretty-print false
                      :foreign-libs [{:file "src/js/gme/gme.js"
                                      :file-min "src/js/gme/gme.js"
                                      :provides ["Gme"]}]
                      :externs ["src/js/gme/gme-externs.js"]}}}})
