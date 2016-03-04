(ns org.hellodata.async.metrics
  (:require [clojure.core.async :as async :refer [go-loop <!]]
            [clojure.core.async.impl.protocols :refer [Buffer full? remove! add!* close-buf!]]
            [metrics.core :as metrics])
  (:import [com.codahale.metrics Gauge MetricRegistry Counter]))

(defn- remove-buffer-metrics
  [registry name]
  (.remove registry (str name ".size"))
  (.remove registry (str name ".added"))
  (.remove registry (str name ".removed")))

(deftype MeasuredBuffer
    [^MetricRegistry registry ^String name
     ^Counter added-counter ^Counter removed-counter wrapped]
  Buffer
  (full? [_]
    (full? wrapped))
  (remove! [_]
    (let [itm (remove! wrapped)]
      (when-not (nil? itm)
        (.inc removed-counter))
      itm))
  (add!* [_ itm]
    (.inc added-counter)
    (add!* wrapped itm))
  (close-buf! [_]
    (remove-buffer-metrics registry name)
    (close-buf! wrapped))
  clojure.lang.Counted
  (count [_]
    (count wrapped)))

(defn measured-buffer
  "Instrumented wrapper around a core.async `buffer`. Registers
  counters for added and removed items, and a size gauge.

  The following metrics are registered under names starting with
  the given `prefix`:

    - prefix.added: counter of items added to the buffer
    - prefix.removed: counter of items taken from the buffer
    - prefix.size: gauge of items currently in the buffer

  Instrumentation is deregistered when the buffer is closed.

  See also: `measured-chan` for more convenient creation of channels
  with measured buffers.

  Example:
      (async/chan
         (measured-buffer registry
                         \"my.interesting.buffer\"
                         (async/buffer 1))"
  ([^MetricRegistry registry ^String prefix buffer]
   (remove-buffer-metrics registry prefix)
   (let [added-counter (.counter registry (str prefix ".added"))
         removed-counter (.counter registry (str prefix ".removed"))
         measured (->MeasuredBuffer registry prefix added-counter removed-counter buffer)
         size-gauge (reify Gauge
                      (getValue [_]
                        (count buffer)))]
     (.register registry (str prefix ".size") size-gauge)
     measured)))

(defn measured-chan
  "Create a channel with a `measured-buffer`, similar to `async/chan`.
  If no registry is provided, uses `metrics.core/default-registry`.

  `prefix` is used to generate names for the installed metrics. See
  `measured-buffer`.

  Example:

      (measured-chan \"my.interesting.channel\" 10)

      (measured-chan registry \"my.interesting.sliding.channel\"
        (async/sliding-buffer 20))"
  ([^MetricRegistry registry ^String prefix buf-or-n]
   (let [wrapped (if (integer? buf-or-n)
                   (async/buffer buf-or-n)
                   buf-or-n)]
     (async/chan (measured-buffer registry prefix wrapped))))
  ([^String prefix buf-or-n]
   (measured-chan metrics/default-registry prefix buf-or-n)))
