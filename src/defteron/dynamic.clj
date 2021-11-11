(ns defteron.dynamic)

(defn int->varint
  ([i] (int->varint i (vector-of :byte)))
  ([i r]
   (let [will-overflow? (> i 128)]
     (cond->
       (bit-and 2r01111111 i)
       will-overflow? (bit-or 2r10000000)
       true (->> (conj r))
       ;;will-overflow? (->> (recur (bit-shift-right i 7)))
       ))))

(defn varint->int
  ([i] (varint->int i 0))
  ([i a]
   (let [base (bit-and i 2r01111111)
         r (bit-shift-right i 8)]
     (cond->> base
       true (bit-or (bit-shift-left a 7))
       (pos? r) (recur r)))))

(def wire-types
  {:varint 0
   :64-bit 1
   :length-delimited 2
   :start-group 3
   :end-group 4
   :32-bit 5})

(defn encode-key [type index]
  (bit-or (get wire-types type) (bit-shift-left index 3)))


(comment
  
  (int->varint 150)

"10010110"

  (bit-or 2r10000000 (bit-and 2r01111111 150))
  (Integer/toString 150 2)
  (bit-shift-right 150 7)

  )

