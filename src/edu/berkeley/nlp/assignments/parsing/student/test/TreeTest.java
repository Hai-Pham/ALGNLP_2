package edu.berkeley.nlp.assignments.parsing.student.test;

import edu.berkeley.nlp.ling.Tree;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


/**
 * Created by Gorilla on 10/21/2016.
 */
public class TreeTest {
    public static void main(String[] args) {
        Tree<String> t1 = new Tree<>("this");
        Tree<String> t2 = new Tree<>("is");
        Tree<String> t3 = new Tree<>("a");
        Tree<String> t4 = new Tree<>("test");

        ArrayList<Tree<String>> l1 = new ArrayList<>();
        l1.add(t1);
        l1.add(t2);

        Tree<String> tree = new Tree("Root", l1);
        System.out.println(tree);

        Tree<String> t5 = new Tree<>(t2.getLabel(), Arrays.asList(t3));
        Tree<String> t6 = new Tree<>(t1.getLabel(), Arrays.asList(t4));

        Tree<String> tree2 = new Tree(tree.getLabel(), Arrays.asList(t5, t6));
        System.out.println(tree2);

        tree.setChildren(Arrays.asList(t5, t6));
        System.out.println(tree);
    }
}
