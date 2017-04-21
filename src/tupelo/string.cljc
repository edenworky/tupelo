;   Copyright (c) Alan Thompson. All rights reserved.
;   The use and distribution terms for this software are covered by the Eclipse Public License 1.0
;   (http://opensource.org/licenses/eclipse-1.0.php) which can be found in the file epl-v10.html at
;   the root of this distribution.  By using this software in any fashion, you are agreeing to be
;   bound by the terms of this license.  You must not remove this notice, or any other, from this
;   software.
(ns tupelo.string
  "Tupelo - Making Clojure even sweeter"
  (:refer-clojure :exclude [drop take] )
  (:require
    [clojure.core :as cc]
    [clojure.string :as str]
    [potemkin.namespaces :as pns]
    [schema.core :as s]
    [tupelo.impl :as i]
    [tupelo.schema :as tsk]))

(pns/import-fn i/char-seq)

; #todo: docstrings
(s/def chars-whitespace-horiz   :- tsk/Set
  (set [\space \tab]))

(s/def chars-whitespace-eol     :- tsk/Set
  (set [\return \newline]))

(s/def chars-whitespace         :- tsk/Set
  (i/glue chars-whitespace-horiz chars-whitespace-eol))

(s/def chars-lowercase          :- tsk/Set
  (into (sorted-set) (char-seq \a \z)))

(s/def chars-uppercase          :- tsk/Set
  (into (sorted-set) (char-seq \A \Z)))

(s/def chars-digit              :- tsk/Set
  (into (sorted-set) (char-seq \0 \9)))

(s/def chars-alpha              :- tsk/Set
  (i/glue chars-lowercase chars-uppercase ))

(s/def chars-alphanumeric       :- tsk/Set
  (i/glue chars-alpha chars-digit ))

(s/def chars-visible  :- tsk/Set
  "Set of all visible (printing) ASCII chars from exclamation point (33) to tilde (126). Excludes all whitespace & control chars."
  (into (sorted-set) (mapv char (i/thru 33 126))))

(s/def chars-text   :- tsk/Set
  "Set of chars used in 'normal' text. Includes all visible chars plus whitespace & EOL chars."
  (i/glue chars-visible chars-whitespace))

(defn alphanumeric?       [& args] (every? #(contains? chars-alphanumeric %) (i/strcat args)))
(defn whitespace-horiz?   [& args] (every? #(contains? chars-whitespace-horiz %) (i/strcat args)))
(defn whitespace-eol?     [& args] (every? #(contains? chars-whitespace-eol %) (i/strcat args)))
(defn whitespace?         [& args] (every? #(contains? chars-whitespace %) (i/strcat args)))
(defn lowercase?          [& args] (every? #(contains? chars-lowercase %) (i/strcat args)))
(defn uppercase?          [& args] (every? #(contains? chars-uppercase %) (i/strcat args)))
(defn digit?              [& args] (every? #(contains? chars-digit %) (i/strcat args)))
(defn alpha?              [& args] (every? #(contains? chars-alpha %) (i/strcat args)))
(defn visible?            [& args] (every? #(contains? chars-visible %) (i/strcat args)))
(defn text?               [& args] (every? #(contains? chars-text %) (i/strcat args)))

; #todo make general version vec -> vec; str-specific version str -> str
; #todo need (substring {:start I :stop J                 } ) ; half-open (or :stop)
; #todo need (substring {:start I :stop J :inclusive true } ) ; closed interval
; #todo need (substring {:start I :count N })

; #todo need (idx "abcdef" 2) -> [ \c ]
; #todo need (indexes "abcde" [1 3 5]) -> (mapv #(idx "abcde" %) [1 3 5]) -> [ \b \d \f ]
; #todo need (idxs    "abcde" [1 3 5]) -> (mapv #(idx "abcde" %) [1 3 5])   ; like matlab

; #todo -> tupelo.string
(defn collapse-whitespace ; #todo readme & blog
  "Replaces all consecutive runs of whitespace characters (including newlines) with a single space.
   Removes any leading or trailing whitespace. Returns a string composed of all tokens
   separated by a single space."
  [it]
  (-> it
    str/trim
    (str/replace #"\s+" " ")))

(s/defn equals-ignore-spacing :- s/Bool  ; #todo readme & blog
  "Compares arguments for equality using tupelo.misc/collapse-whitespace.
   Equivalent to separating tokens by whitespace and comparing the resulting sequences."
  [& args :- [s/Str]]
  (let [ws-collapsed-args (mapv collapse-whitespace args)]
    (apply = ws-collapsed-args)))

; #todo need (squash)         -> (collapse-whitespace (strcat args))       ; (smash ...)         ?
; #todo need (squash-equals?) -> (apply = (mapv squash args))              ; (smash-equals? ...)  ?
;    or (equals-base) or (equals-root) or (squash-equals) or (base-equals) or (core-equals) or (equals-collapse-string...)

(s/defn quotes->single :- s/Str ; #todo readme & blog
  [arg :- s/Str]
  (str/replace arg \" \'))

(s/defn quotes->double :- s/Str ; #todo readme & blog
  [arg :- s/Str]
  (str/replace arg \' \"))

(defn ^:deprecated ^:no-doc double-quotes->single-quotes [& args] (apply quotes->single args))
(defn ^:deprecated ^:no-doc single-quotes->double-quotes [& args] (apply quotes->double args))

; #todo need tests
(defn normalize-str
  "Returns a 'normalized' version of str-in, stripped of leading/trailing
   blanks, and with all non-alphanumeric chars converted to hyphens."
  [str-in]
  (-> str-in
    str/trim
    (str/replace #"[^a-zA-Z0-9]" "-")))
; #todo replace with other lib

; %todo define current mode only for (str->kw "ab*cd #()xyz" :sloppy), else throw
(defn str->kw-normalized       ; #todo need test, README
  "Returns a keyword constructed from a normalized string"
  [arg]
  (keyword (normalize-str arg)))

; #todo throw if bad string
(defn str->kw       ; #todo need test, README
  "Returns a keyword constructed from a normalized string"
  [arg]
  (keyword arg))

(defn kw->str       ; #todo need test, README
  "Returns the string version of a keyword, stripped of the leading ':' (colon)."
  [arg]
  (str/join (cc/drop 1 (str arg))))

(defn snake->kabob
  "Converts a string from a_snake_case_value to a-kabob-case-value"
  [arg]
  (str/replace arg \_ \- ))

(defn kabob->snake
  "Converts a string from a-kabob-case-value to a_snake_case_value"
  [arg]
  (str/replace arg \- \_ ))

(defn kw-snake->kabob [kw]
  (-> kw
    (kw->str)
    (snake->kabob)
    (str->kw)))

(defn kw-kabob->snake [kw]
  (->> kw
    (kw->str)
    (kabob->snake)
    (str->kw)))

;-----------------------------------------------------------------------------

(s/defn drop :- s/Str  ; #todo add readme
  "Drops the first N chars of a string, returning a string result."
  [n    :- s/Int
   txt  :- s/Str]
  (str/join (cc/drop n txt)))

(s/defn take :- s/Str  ; #todo add readme
  "Drops the first N chars of a string, returning a string result."
  [n    :- s/Int
   txt  :- s/Str]
  (str/join (cc/take n txt)))

(s/defn indent :- s/Str  ; #todo add readme
  "Indents a string by pre-pending N spaces. Returns a string result."
  [n    :- s/Int
   txt  :- s/Str]
  (let [indent-str (str/join (repeat n \space))]
    (str indent-str txt)))

(s/defn indent-lines :- s/Str  ; #todo add readme
  "Splits out each line of txt using clojure.string/split-lines, then
  indents each line by prepending N spaces. Joins lines together into
  a single string result, with each line terminated by a single \newline."
  [n    :- s/Int
   txt  :- s/Str]
  (str/join
    (for [line (str/split-lines txt) ]
      (str (indent n line) \newline))))

(s/defn increasing :- s/Bool
  "Returns true if a pair of strings are in increasing lexicographic order."
  [a :- s/Str
   b :- s/Str ]
  (neg? (compare a b)))

(s/defn increasing-or-equal :- s/Bool
  "Returns true if a pair of strings are in increasing lexicographic order, or equal."
  [a :- s/Str
   b :- s/Str ]
  (or (= a b)
      (increasing a b)))

(defn index-of [search-str tgt-str]
  (.indexOf search-str tgt-str))

(defn starts-with? [search-str tgt-str]
  (zero? (index-of search-str tgt-str)))

; #todo add undent (verify only leading whitespace removed)
; #todo add undent-lines