package edu.berkeley.nlp.assignments.parsing.student.util;

/**
 * Helper class to score the binary back pointer in CKY parsing score table
 * Created by Gorilla on 10/19/2016.
 */
public class Triple<T1, T2, T3> {
    // avoid setters ang getters
    public T1 split;
    public T2 begin;
    public T3 end;

    public Triple(T1 s, T2 b, T3 e) {
        split = s;
        begin = b;
        end = e;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Triple triple = (Triple) o;

        return (split.equals(triple.split)) & (begin.equals(triple.begin))
                & (end.equals(triple.end));

    }

    @Override
    public int hashCode() {
        int result = split.hashCode();
        result = 31 * result + (begin != null ? begin.hashCode() : 0);
        result = 31 * result + (end != null ? end.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "Triple{" +
                "split=" + split +
                ", begin='" + begin + '\'' +
                ", end='" + end + '\'' +
                '}';
    }

    public static void main(String[] args) {
        Triple<Integer, String, String> t1 = new Triple<>(1, "a", "c");
        Triple<Integer, String, String> t2 = new Triple<>(2, "b", "c");
        Triple<Integer, String, String> t3 = new Triple<>(1, "a", "c");

        System.out.println(t1 == t2);
        System.out.println(t1 == t3);
        System.out.println(t1.equals(t2));
        System.out.println(t1.equals(t3));
    }
}
