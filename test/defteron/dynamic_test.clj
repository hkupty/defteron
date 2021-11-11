(ns defteron.dynamic-test
  (:require [clojure.test :as t]
            [defteron.dynamic :as d]))

(t/deftest int<->varint
  (t/testing "less than 7 bits remains the same"
    (t/is (= 1 (d/int->varint 1)))
    (t/is (= 127 (d/int->varint 127))))
  (t/testing "after 7 bits number gets shifted"
    (t/is (= 2r1010110000000010 (d/int->varint 300))))
  (t/testing "we can get back the same numbers"
    (t/is (= 300 (d/varint->int 2r1010110000000010)))
    (t/is (= 1 (d/varint->int 1)))
    (t/is (= 127 (d/varint->int 127)))))
