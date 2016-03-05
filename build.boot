(set-env! :dependencies '[[adzerk/boot-cljs "1.7.228-1"]
                          [boot-deps "0.1.6"]
                          [cljs-http "0.1.39"]
                          [clojurewerkz/balagan "1.0.5"]
                          [org.clojure/clojure "1.8.0"]
                          [org.clojure/clojurescript "1.7.228"]
                          [org.clojure/core.async "0.2.374"]
                          [pandeiro/boot-http "0.7.3"]
                          [prismatic/dommy "1.1.0"]]
          :resource-paths #{"assets"}
          :source-paths #{"src"})

(require '[adzerk.boot-cljs :refer [cljs]]
         '[boot-deps :refer [ancient]]
         '[pandeiro.boot-http :refer [serve]])

(deftask develop
  []
  (comp
    (serve :dir "target" :port 3000)
    (watch)
    (speak)
    (cljs :optimizations :whitespace)))

(deftask release
  []
  (comp
    (speak)
    (cljs :optimizations :advanced)
    (with-post-wrap fileset
      (dosh "mkdir" "-p" "gh-pages")
      (doseq [file #{"target/main.js" "assets/index.html"}]
        (dosh "cp" "-f" file "gh-pages")))))
