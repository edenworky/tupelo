(ns ^:test-refresh/focus
  tst.tupelo.java-time.epoch
  (:refer-clojure :exclude [range])
  (:use tupelo.java-time.epoch tupelo.core tupelo.test)
  (:require
    [clj-time.core :as joda]
    [tupelo.core :as t]
    [tupelo.interval :as interval]
    [tupelo.string :as str]
    [schema.core :as s]
    [tupelo.schema :as tsk])
  (:import
    [java.time Duration Instant MonthDay YearMonth LocalDate LocalDateTime Period
               ZoneId ZoneId ZonedDateTime]
    [java.util Date]
    ))

(dotest ; LocalDate <==> eday
  (is= {:eday 0} (LocalDate->eday (LocalDate/parse "1970-01-01")))
  (is= {:eday 1} (LocalDate->eday (LocalDate/parse "1970-01-02")))
  (is= {:eday 31} (LocalDate->eday (LocalDate/parse "1970-02-01")))
  (is= {:eday 365} (LocalDate->eday (LocalDate/parse "1971-01-01")))
  (doseq [ld-str ["1970-01-01"
                  "1970-01-02"
                  "1970-02-01"
                  "1971-01-01"
                  "1999-12-31"]]
    (let [ld (LocalDate/parse ld-str)]
      (is= ld (-> ld (LocalDate->eday) (eday->LocalDate)))))
  (doseq [daynum [0 1 9 99 999 9999]]
    (is= {:eday daynum} (-> daynum (->eday) (eday->LocalDate) (LocalDate->eday)))))

(dotest
  (let [ldstr->monthVal (fn [arg] (-> arg (LocalDateStr->eday) (eday->monthValue)))]
    (is= 1 (ldstr->monthVal "2013-01-25"))
    (is= 2 (ldstr->monthVal "2013-02-28"))
    (is= 11 (ldstr->monthVal "2013-11-30"))
    (is= 12 (ldstr->monthVal "2013-12-31"))))

(dotest
  (let [ldstr->year (fn [arg] (-> arg (LocalDateStr->eday) (eday->year)))]
    (is= 2013 (ldstr->year "2013-01-25"))
    (is= 2014 (ldstr->year "2014-02-28"))
    (is= 2014 (ldstr->year "2014-11-30"))
    (is= 2019 (ldstr->year "2019-12-31"))))

(dotest
  (is= {:eday 9134} (LocalDateStr->eday "1995-01-04"))
  (is= {:eday 10956} (LocalDateStr->eday "1999-12-31"))
  (doseq [daynum [0 9 99 999 9999]]
    (is= {:eday daynum} (-> daynum (->eday) (eday->LocalDateStr) (LocalDateStr->eday)))))

