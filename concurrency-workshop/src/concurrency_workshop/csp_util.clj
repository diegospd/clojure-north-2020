(ns concurrency-workshop.csp_util
  (:require [taoensso.nippy :as nippy]
            [clj-time.core :as t]
            [pandect.algo.sha1 :as sha1]))


(defn make-data []
  (let [raw {:id (.toString (java.util.UUID/randomUUID))
             :s (rand-nth [:temp :humidity :wind])                ;; source
             :l (rand-nth ["SanFrancisco" "NewYork" "Toronto" "Pune"]) ;; location
             :t (.toString (t/now))                               ;; time
             :v (- (rand-int 100) 25)}]
    (assoc raw :h (sha1/sha1-hmac (str raw) "clojure-north"))))


(defn serialise [data]
  (nippy/freeze data))


(defn deserialise [data]
  (nippy/thaw data))


(defn authorize [data]
  (let [sign (sha1/sha1-hmac (str (dissoc data :h))
                             "clojure-north")]
    (when (= sign (:h data))
      data)))


(defn transform [data mapping]
  (let [xform (comp
               (filter (fn [m]
                         (contains? mapping (first m))))
               (map (fn [m]
                      [(mapping (first m)) (second m)])))]
    (into {} xform data)))

(defn populate [data]
  (transform data {:id :id
                   :s :source
                   :l :location
                   :t :time
                   :v :value
                   :h :signature}))


(defn validate [data]
  (if (or
       (= (:source data) :temp)
       (and (= (:source data) :wind)
            ((fnil #(>= % 0) 0) (:value data)))
       (and (= (:source data) :humidity)
            ((fnil #(<= % 100) 100) (:value data))
            ((fnil #(> % 0) 0) (:value data))))
    data
    nil))


(defn stream [data]
  (when data
   (let [topic-mapping {:temp "temperatures"
                        :humidity "humidity"
                        :wind "wind_speed"}]
     (println (format "Sending %s to kafka topic : %s"
                      data
                      (topic-mapping (:source data)))))))
