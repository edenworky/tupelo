;   Copyright (c) Alan Thompson. All rights reserved.
;   The use and distribution terms for this software are covered by the Eclipse Public
;   License 1.0 (http://opensource.org/licenses/eclipse-1.0.php) which can be found in the
;   file epl-v10.html at the root of this distribution.  By using this software in any
;   fashion, you are agreeing to be bound by the terms of this license.
;   You must not remove this notice, or any other, from this software.
(ns tupelo.array.mutable
  (:require
    [clojure.set :as set]
    [clojure.string :as str]
    [schema.core :as s]
    [tupelo.string :as ts]

    #?(:clj  [tupelo.core :as t :refer [spy spyx spyxx spy-pretty spyx-pretty forv vals->map glue truthy? falsey?]]
       :cljs [tupelo.core :as t :include-macros true
              :refer [spy spyx spyxx spy-pretty spyx-pretty forv vals->map glue truthy? falsey?]])
    )
  (:import [java.util Arrays]))

#?(:clj
   (do    ; #todo fix this

     (def Vector
       "Plumatic Schema type definition for a 1-D array of values (a vector of values)."
       [s/Any])

     (def Array
       "Plumatic Schema type definition for a 2-D array of values (a vector of vectors)."
       {:data  s/Any
        :nrows s/Int
        :ncols s/Int})

     (s/defn num-rows :- s/Int
       "Returns the number of rows of an Array."
       [arr :- Array]
       (:nrows arr))

     (s/defn num-cols :- s/Int
       "Returns the number of cols of an Array."
       [arr :- Array]
       (:ncols arr))

     (s/defn ^:no-doc check-row-idx
       [arr :- Array
        idx :- s/Int]
       (let [limit (num-rows arr)]
         (when-not (< -1 idx limit)
           (throw (ex-info "Row index out of range" (vals->map idx limit))))))

     (s/defn ^:no-doc check-col-idx
       [arr :- Array
        idx :- s/Int]
       (let [limit (num-cols arr)]
         (when-not (< -1 idx limit)
           (throw (ex-info "Col index out of range" (vals->map idx limit))))))

     (s/defn ^:no-doc check-array-indexes
       [arr :- Array
        ii :- s/Int
        jj :- s/Int]
       (check-row-idx arr ii)
       (check-col-idx arr jj))

     (s/defn ^:no-doc idx :- s/Int
       [arr :- Array
        ii :- s/Int
        jj :- s/Int]
       (check-array-indexes arr ii jj)
       (+ (* ii (:ncols arr)) jj))

     (s/defn create :- Array
       "Return a new Array (vector-of-vectors) of size=[nrows ncols], initialized to `init-val` (default=nil)"
       ([nrows :- s/Int
         ncols :- s/Int]
        (create nrows ncols nil))
       ([nrows :- s/Int
         ncols :- s/Int
         init-val :- s/Any]
        (assert (and (pos? nrows) (pos? ncols)))
        (let [num-elems (* nrows ncols)
              result (glue
                       (vals->map nrows ncols)
                       {:data (object-array num-elems)})]
          (Arrays/fill (:data result) init-val)
          result)))

     (s/defn elem-get :- s/Any
       "Gets an Array element"
       [arr :- Array
        ii :- s/Int
        jj :- s/Int]
       (aget (:data arr) (idx arr ii jj)))

     (s/defn elem-set :- Array
       "Puts a value into an Array element, returning the updated Array."
       [arr :- Array
        ii :- s/Int
        jj :- s/Int
        newVal :- s/Any]
       (aset (:data arr) (idx arr ii jj) newVal)
       arr)

     (s/defn array->str :- s/Str
       "Returns a string representation of an array"
       [arr :- Array]
       (let [result (str/join
                      (flatten
                        (for [ii (range (num-rows arr))]
                          (t/append
                            (for [jj (range (num-cols arr))]
                              (let [val (str (elem-get arr ii jj))]
                                (ts/pad-left val 8)))
                            \newline))))]
         result))

     (s/defn array->edn :- [[s/Any]]
       "Returns a persistant EDN data structure (vector-of-vectors) from the array."
       [arr :- Array]
       (mapv vec (partition (:ncols arr) (:data arr))))

     (s/defn rows->array :- Array
       "Return a new Array initialized from row-vecs. Rows must all have same length."
       [row-vecs :- [[s/Any]]]
       (let [nrows (count row-vecs)
             ncols (count (first row-vecs))]
         (assert (apply = ncols (mapv count row-vecs)))
         (dotimes [ii nrows]
           (assert sequential? (nth row-vecs ii)))
         (let [arr (create nrows ncols)]
           (dotimes [ii nrows]
             (dotimes [jj ncols]
               (elem-set arr ii jj
                 (get-in row-vecs [ii jj]))))
           arr)))

     (s/defn edn->array :- Array
       "Synonym for rows->array"
       [row-vecs :- [[s/Any]]]
       (rows->array row-vecs))

     (s/defn equals :- s/Bool
       "Returns true if two Arrays contain equal data"
       [x :- Array
        y :- Array]
       (and
         (= (:nrows x) (:nrows y))
         (= (:ncols x) (:ncols y))
         (= (seq (:data x)) (seq (:data y)))))

     (s/defn row-get :- Vector
       "Gets an Array row"
       [arr :- Array
        ii :- s/Int]
       (check-row-idx arr ii)
       (forv [jj (range (num-cols arr))]
         (elem-get arr ii jj)))

     (s/defn col-get :- Vector
       "Gets an Array col"
       [arr :- Array
        jj :- s/Int]
       (check-col-idx arr jj)
       (forv [ii (range (num-rows arr))]
         (elem-get arr ii jj)))

     (s/defn array->row-vals :- Vector
       "Returns the concatenation of all array rows."
       [arr :- Array]
       (forv [ii (range (num-rows arr))
              jj (range (num-cols arr))]
         (elem-get arr ii jj)))

     (s/defn array->col-vals :- Vector
       "Returns the concatenation of all array cols."
       [arr :- Array]
       (forv [jj (range (num-cols arr))
              ii (range (num-rows arr))]
         (elem-get arr ii jj)))

     (s/defn row-vals->array :- Array
       "Return a new Array of size=[nrows ncols] with its rows constructed from from row-data."
       [nrows :- s/Int
        ncols :- s/Int
        row-data :- Vector]
       (assert (and (pos? nrows) (pos? ncols)))
       (assert (= (* nrows ncols) (count row-data)))
       (let [result (glue
                      (vals->map nrows ncols)
                      {:data (object-array row-data)})]
         result))

     (s/defn transpose :- Array
       "Returns the transpose of an array. Returns a new array."
       [orig :- Array]
       (row-vals->array (:ncols orig) (:nrows orig)
         (array->col-vals orig)))

     (s/defn col-vals->array :- Array
       "Return a new Array of size=[nrows ncols] with its columns constructed from from col-data."
       [nrows :- s/Int
        ncols :- s/Int
        col-data :- Vector]
       (assert (and (pos? nrows) (pos? ncols)))
       (assert (= (* nrows ncols) (count col-data)))
       (let [result (create nrows ncols)]
         (dotimes [ii nrows]
           (dotimes [jj ncols]
             (elem-set result ii jj
               (nth col-data (+ ii (* jj nrows))))))
         result))

     (s/defn flip-ud :- Array
       "Flips an array in the up-down direction,
       reversing the order of the rows of an array. Returns a new array."
       [orig :- Array]
       (let [nrows  (:nrows orig)
             ncols  (:ncols orig)
             result (create nrows ncols)]
         (dotimes [ii nrows]
           (let [ii-orig (- nrows ii 1)]
             (dotimes [jj ncols]
               (elem-set result ii jj
                 (elem-get orig ii-orig jj)))))
         result))

     (s/defn flip-lr :- Array
       "Flips an array in the left-right direction,
       reversing the order of the cols of an array. Returns a new array."
       [orig :- Array]
       (let [nrows  (:nrows orig)
             ncols  (:ncols orig)
             result (create nrows ncols)]
         (dotimes [jj ncols]
           (let [jj-orig (- ncols jj 1)]
             (dotimes [ii nrows]
               (elem-set result ii jj
                 (elem-get orig ii jj-orig)))))
         result))

     (s/defn rotate-left :- Array
       "Rotates an array 90 deg counter-clockwise. Returns a new array."
       [orig :- Array]
       (rows->array
         (forv [jj (reverse (range (num-cols orig)))]
           (col-get orig jj))))

     (s/defn rotate-right :- Array
       "Rotates an array 90 deg clockwise. Returns a new array."
       [orig :- Array]
       (rows->array
         (forv [jj (range (num-cols orig))]
           (vec (reverse (col-get orig jj)))))) ; reverse yields a seq, not a vec! doh!

     ;#todo make both rows/cols -> submatrix result
     (s/defn array->rows :- [[s/Any]]
       "
        [arr]           Returns all array rows
        [arr row-idxs]  Returns array rows specified by row-idxs
        [arr low high]  Returns array rows in half-open interval [low..high) "
       ([arr] (array->rows arr 0 (num-rows arr)))
       ([arr
         row-idxs :- [s/Int]]
        (forv [ii row-idxs]
          (row-get arr ii)))
       ([arr :- Array
         low :- s/Int
         high :- s/Int]
        (check-row-idx arr low)
        (check-row-idx arr (dec high))
        (assert (< low high))
        (forv [ii (range low high)]
          (row-get arr ii))))
     ; #todo need parallel rows-set

     (comment


       (s/defn cols->array :- Array
         "[col-vecs]
         Return a new Array initialized from col-vecs. Cols must all have same length."
         [col-vecs :- Array]
         (let [ncols (count col-vecs)
               nrows (count (first col-vecs))]
           (assert (apply = nrows (mapv count col-vecs)))
           (dotimes [jj ncols]
             (assert sequential? (nth col-vecs jj)))
           (col-vals->array nrows ncols (apply glue col-vecs))))

       (s/defn row-set :- Array
         "Sets an Array row"
         [orig :- Array
          ii :- s/Int
          new-row :- Vector]
         (check-row-idx orig ii)
         (assert (= (num-cols orig) (count new-row)))
         (let [nrows  (num-rows orig)
               result (glue
                        (forv [ii (range ii)] (row-get orig ii))
                        [new-row]
                        (forv [ii (range (inc ii) nrows)] (row-get orig ii)))]
           result))

       (s/defn col-set :- Array
         "Sets an Array col"
         [orig :- Array
          jj :- s/Int
          new-col :- Vector]
         (check-col-idx orig jj)
         (let [nrows  (num-rows orig)
               >>     (assert (= nrows (count new-col)))
               result (forv [ii (range nrows)]
                        (let [curr-row (row-get orig ii)
                              new-val  (nth new-col ii)
                              new-row  (t/replace-at curr-row jj new-val)]
                          new-row))]
           result))

       (s/defn array->cols :- Array
         "
          [arr]           Returns all array cols
          [arr col-idxs]  Returns array cols specified by col-idxs
          [arr low high]  Returns array cols in half-open interval [low..high) "
         ([arr] (array->cols arr 0 (num-cols arr)))
         ([arr col-idxs]
          (forv [jj col-idxs]
            (col-get arr jj)))
         ([arr :- Array
           low :- s/Int
           high :- s/Int]
          (check-col-idx arr low)
          (check-col-idx arr (dec high))
          (assert (< low high))
          (forv [jj (range low high)]
            (col-get arr jj))))
       ; #todo need parallel cols-set

       (s/defn symmetric? :- s/Bool
         "Returns true iff an array is symmetric"
         [arr :- Array]
         (let [nrows (num-rows arr)
               ncols (num-cols arr)]
           (and (= nrows ncols)
             (every? truthy?
               (for [ii (range 0 nrows)
                     jj (range ii ncols)] (= (elem-get arr ii jj)
                                            (elem-get arr jj ii)))))))

       (s/defn row-drop :- Array
         "Drop one or more rows from an array"
         [orig :- Array
          & idxs-drop :- [s/Int]]
         (let [idxs-all  (set (range (num-rows orig)))
               idxs-drop (set idxs-drop)
               idxs-keep (sort (set/difference idxs-all idxs-drop))]
           (forv [ii idxs-keep]
             (row-get orig ii))))

       (s/defn col-drop :- Array
         "Drop one or more colss from an array"
         [orig :- Array
          & idxs-drop :- [s/Int]]
         (let [idxs-all  (set (range (num-cols orig)))
               idxs-drop (set idxs-drop)
               idxs-keep (sort (set/difference idxs-all idxs-drop))]
           (forv [ii (range (num-rows orig))]
             (forv [jj idxs-keep]
               (elem-get orig ii jj)))))

       (s/defn row-add :- Array
         "Appends a new row onto an array."
         [orig :- Array
          & rows :- [Vector]]
         (let [row-lens (mapv count rows)]
           (assert (apply = (num-cols orig) row-lens)))
         (into orig rows))

       (s/defn col-add :- Array
         "Appends a new col onto an array."
         [orig :- Array
          & cols :- [Vector]]
         (let [nrows    (num-rows orig)
               col-lens (mapv count cols)]
           (assert (apply = nrows col-lens))
           (forv [ii (range nrows)]
             (glue (row-get orig ii) (forv [col cols]
                                       (nth col ii))))))

       (s/defn glue-vert :- Array
         "Concatenates 2 or more arrays vertically. Arrays must all have the same number of cols."
         [& arrays :- [Array]]
         (assert (pos? (count arrays)))
         (let [ncol-vals (mapv num-cols arrays)]
           (assert (apply = ncol-vals)))
         (apply glue arrays))

       (s/defn glue-horiz :- Array
         "Concatenates 2 or more arrays horizontally. Arrays must all have the same number of rows."
         [& arrays :- [Array]]
         (assert (pos? (count arrays)))
         (let [nrow-vals (mapv num-rows arrays)]
           (assert (apply = nrow-vals)))
         (forv [ii (range (num-rows (t/xfirst arrays)))]
           (apply glue (mapv #(row-get % ii) arrays))))

       )

     ))


