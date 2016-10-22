package edu.berkeley.nlp.assignments.parsing.student.test;

/**
 * Created by Gorilla on 10/21/2016.
 */
public class SmallSandboxTester {
    public static class A {
        int aa;

        public A(int a) {
            aa = a;
        }
        public int getA() {
            return aa;
        }

        public void setA(int a) {
            aa = a;
        }
    }

    private static A a;

    public static void main(String[] args) {
        a = new A(1);
        System.out.println(a.getA());

        a = new A(2);
        System.out.println(a.getA());
    }
}
