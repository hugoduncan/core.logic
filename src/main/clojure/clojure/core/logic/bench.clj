(ns clojure.core.logic.bench
  (:refer-clojure :exclude [reify inc ==])
  (:use clojure.core.logic.minikanren
        [clojure.core.logic.prelude :only [firsto defne]]
        [clojure.core.logic.disequality :only [!=]])
  (:require [clojure.core.logic.nonrel :as nonrel]
            [clojure.contrib.macro-utils :as macro]))

;; =============================================================================
;; Utilities

(defne membero [x l]
  ([_ [x . ?tail]])
  ([_ [?head . ?tail]]
     (membero x ?tail)))

;; =============================================================================
;; flatten
;; =============================================================================

;; =============================================================================
;; append

(defne appendo [x y z]
  ([() _ y])
  ([[?a . ?d] _ [?a . ?r]] (appendo ?d y ?r)))

(comment
  (run 1 [q]
    (exist [x y]
      (appendo x y q)))
  
  ;; 1.4s
  (dotimes [_ 10]
    (time
     (dotimes [_ 1]
       (doall
        (run 700 [q]
          (exist [x y]
            (appendo x y q)))))))
  )

;; =============================================================================
;; nrev
;; =============================================================================

(defne nrevo [l o]
  ([() ()])
  ([[?a . ?d] _]
     (exist [r]
       (nrevo ?d r)
       (appendo r [?a] o))))

(comment
  ;; we can run backwards, unlike Prolog
  (run 1 [q] (nrevo q (range 30)))

  ;; SWI-Prolog 0.06-0.08s
  ;; ~4.1s
  (let [data (into [] (range 30))]
    (binding [*occurs-check* false]
      (dotimes [_ 5]
        (time
         (dotimes [_ 1e3]
           (doall (run 1 [q] (nrevo data q))))))))

  ;; the LIPS are ridiculously high for SWI-Prolog
  ;; clearly nrev is a case that SWI-Prolog can optimize away
  )

;; =============================================================================
;; zebra
;; =============================================================================

(defne righto [x y l]
  ([_ _ [x y . ?r]])
  ([_ _ [_ . ?r]] (righto x y ?r)))

(defn nexto [x y l]
  (conde
    ((righto x y l))
    ((righto y x l))))

(defn zebrao [hs]
  (macro/symbol-macrolet [_ (lvar)]
    (all
     (== [_ _ [_ _ 'milk _ _] _ _] hs)                         
     (firsto hs ['norwegian _ _ _ _])                         
     (nexto ['norwegian _ _ _ _] [_ _ _ _ 'blue] hs)       
     (righto [_ _ _ _ 'ivory] [_ _ _ _ 'green] hs)         
     (membero ['englishman _ _ _ 'red] hs)                    
     (membero [_ 'kools _ _ 'yellow] hs)                      
     (membero ['spaniard _ _ 'dog _] hs)                      
     (membero [_ _ 'coffee _ 'green] hs)                      
     (membero ['ukrainian _ 'tea _ _] hs)                     
     (membero [_ 'lucky-strikes 'oj _ _] hs)                  
     (membero ['japanese 'parliaments _ _ _] hs)              
     (membero [_ 'oldgolds _ 'snails _] hs)                   
     (nexto [_ _ _ 'horse _] [_ 'kools _ _ _] hs)          
     (nexto [_ _ _ 'fox _] [_ 'chesterfields _ _ _] hs))))

(comment
  ;; SWI-Prolog 6-8.5s
  ;; ~2.4s
  (binding [*occurs-check* false]
    (dotimes [_ 5]
      (time
       (dotimes [_ 1e3]
         (doall (run 1 [q] (zebrao q)))))))

  ;; < 3s
  (dotimes [_ 5]
    (time
     (dotimes [_ 1e3]
       (doall (run 1 [q] (zebrao q))))))
  )

;; =============================================================================
;; nqueens

;; Bratko pg 103

(declare noattacko)

(defne nqueenso [l]
  ([()])
  ([[[?x ?y] . ?others]]
     (nqueenso ?others)
     (membero ?y [1 2 3 4 5 6 7 8])
     (noattacko [?x ?y] ?others)))

(defne noattacko [q others]
  ([_ ()])
  ([[?x ?y] [[?x1 ?y1] . ?others]]
     (!= ?y ?y1)
     (nonrel/project [?y ?y1 ?x ?x1]
       (!= (- ?y1 ?y) (- ?x1 ?x))
       (!= (- ?y1 ?y) (- ?x ?x1)))
     (noattacko [?x ?y] ?others)))

(defn solve-nqueens []
  (run* [q]
    (exist [y1 y2 y3 y4 y5 y6 y7 y8]
      (== q [[1 y1] [2 y2] [3 y3] [4 y4] [5 y5] [6 y6] [7 y7] [8 y8]])
      (nqueenso q))))

(comment
  (take 1 (solve-nqueens))

  ;; 92 solutions
  (count (solve-nqueens))

  ;; < 3s for 100x
  ;; about 18X slower that SWI
  (binding [*occurs-check* false]
    (dotimes [_ 5]
      (time
       (dotimes [_ 1]
         (doall
          (take 1 (solve-nqueens)))))))

  ;; ~550ms
  (binding [*occurs-check* false]
    (dotimes [_ 10]
      (time
       (dotimes [_ 1]
         (doall
          (solve-nqueens))))))

  ;; ~610ms
  (dotimes [_ 10]
    (time
     (dotimes [_ 1]
       (doall
        (solve-nqueens)))))

  ;; nqueens benefits from constraints
  )

;; Bratko pg 344, constraint version

;; =============================================================================
;; send more money

(defne selecto [x l r]
  ([_ [x . r] _])
  ([_ [?y . ?xs] [?y . ?ys]]
     (selecto x ?xs ?ys)))

(defne assign-digitso [l1 l2]
  ([() _])
  ([[?d . ?ds] _]
     (exist [nl]
       (selecto ?d l2 nl)
       (assign-digitso ?ds nl))))

(defn smm [x]
  (exist [s e n d m o r y digits]
    (== x [s e n d m o r y])
    (== digits (range 10))
    (assign-digitso x digits)
    (nonrel/project [s e n d m o r y]
      (== (> m 0) true)
      (== (> s 0) true)
      (== (+ (* 1e3 s) (* 1e2 e) (* 10 n) d (* 1e3 m) (* 1e2 o) (* 10 r) e)
          (+ (* 1e4 m) (* 1e3 o) (* 1e2 n) (* 10 e) y)))))

(comment
  ;; SWI takes 4.4 seconds

  ;; 113865ms, 113s
  ;; 28X slower
  ;; ([9 5 6 7 1 0 8 2])
  (time
   (doall (run 1 [q]
            (smm q))))

  ;; looking at YourKit the time seems completely dominated by inc (thunking to force interleaving)
  ;; not sure how to get better times w/o some sort of scheduler, a good argument for looking deeper
  ;; at the ferns implementation and determining whether that can be made fast or not.

  ;; send more money also benefits from constraints
  )

;; =============================================================================
;; Quick Sort

(declare partitiono)

(defne qsorto [l r r0]
  ([[] _ r])
  ([[?x . ?lr] _ _]
     (exist [l1 l2 r1]
       (partitiono ?lr ?x l1 l2)
       (qsorto l2 r1 r0)
       (qsorto l1 r (lcons ?x r1)))))

(defne partitiono [a b c d]
  ([[?x . ?l] _ [?x . ?l1] _]
     (nonrel/conda
      ((nonrel/project [?x b]
         (== (<= ?x b) true))
       (partition ?l b ?l1 d))
      (partition ?l b c d))))