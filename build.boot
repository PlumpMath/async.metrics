(set-env!
 :source-paths #{"src" "test"}
 :dependencies '[[adzerk/bootlaces "0.1.13" :scope "test"]
                 [adzerk/boot-test "1.1.0" :scope "test"]
                 [metrics-clojure/metrics-clojure "2.6.1"]
                 [org.clojure/clojure "1.8.0" :scope "provided"]
                 [org.clojure/core.async "0.2.374"]])

(require '[adzerk.boot-test :refer :all])
(require '[adzerk.bootlaces :refer :all])

(def +version+ "0.0.1-SNAPSHOT")
(bootlaces! +version+)

(task-options!
 pom {:project 'mpare-net/async.metrics
      :version +version+
      :description "Codahale metrics for Clojure core.async channels"
      :url         "https://github.com/mpare-net/async.metrics"
      :scm         {:url "https://github.com/mpare-net/async.metrics"}})
