(ns bench.runner
  (:require [criterium.core :as crit]
            [bench.defteron :as lib]
            [bench.native :as native])
  (:gen-class))


(defn -main []
  (set! *warn-on-reflection* true)
  (println ::msg)
  (crit/report-result (crit/quick-benchmark (lib/serialize-msg) {}))
  (crit/report-result (crit/quick-benchmark (native/serialize-msg) {}))

  (println ::enum)
  (crit/report-result (crit/quick-benchmark (lib/serialize-enum-only) {}))
  (crit/report-result (crit/quick-benchmark (native/serialize-enum-only) {})))
