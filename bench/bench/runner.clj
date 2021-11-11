(ns bench.runner
  (:require [criterium.core :as crit]
            [bench.defteron :as lib]
            [bench.native :as native])
  (:import (defteron Proto$Header Proto$Size))
  (:gen-class))

(def base (.build (doto (Proto$Header/newBuilder)
                    (.setMsgSize Proto$Size/large)
                    (.setData "Some data")
                    (.addAllMeta ["a" "really" "short" "list"]))))

(defn -main []
  (println ::sleep-10)
  (Thread/sleep 10000)
  (println ::sleep-20)
  (Thread/sleep 10000)
  (set! *warn-on-reflection* true)
  (println ::msg)
  (crit/report-result (crit/quick-benchmark (lib/serialize-msg) {}))
  (crit/report-result (crit/quick-benchmark (native/serialize-msg) {}))

  (println ::enum)
  (crit/report-result (crit/quick-benchmark (lib/serialize-enum-only) {}))
  (crit/report-result (crit/quick-benchmark (native/serialize-enum-only) {}))

  (println ::msg)
  (crit/report-result (crit/quick-benchmark (lib/deserialize-msg base) {}))
  (crit/report-result (crit/quick-benchmark (native/deserialize-msg base) {})))
