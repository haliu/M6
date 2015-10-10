#!/bin/sh

echo "File Count:"
ls -1 */*.lisp */*/*.lisp | wc -l  
echo "Line Count:"
cat */*.lisp */*/*.lisp | wc -l 
echo "Defun Count:"
cat */*.lisp */*/*.lisp | grep "(defun" | grep -v "^[ ]*;" | wc -l 
echo "Defthm Count:"
cat */*.lisp */*/*.lisp | grep "(defthm" | grep -v "^[ ]*;" | wc -l 
echo "Functions with guards defined":
cat */*.lisp */*/*.lisp | grep -A 3 "(defun" | grep -v "^[ ]*;" | grep ":guard" | wc -l
echo "number of explicit verify guard count:"
cat */*.lisp */*/*.lisp | grep "(verify-guards" | grep -v "^[ ]*;" | wc -l 
echo "Skip proofs Count:"
cat */*.lisp */*/*.lisp | grep "(skip-proofs" | grep -v "^[ ]*;" | wc -l 
echo "Among them, number skip proofs used for verify guards": 
cat */*.lisp */*/*.lisp | grep -A 3 "(skip-proofs" | grep -v "^[ ]*;" | grep "(verify-guards" | wc -l  


