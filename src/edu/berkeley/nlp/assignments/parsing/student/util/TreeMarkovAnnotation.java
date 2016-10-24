package edu.berkeley.nlp.assignments.parsing.student.util;

import edu.berkeley.nlp.ling.Tree;
import edu.berkeley.nlp.ling.Trees;
import edu.berkeley.nlp.util.Filter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Created by Gorilla on 10/23/2016.
 */
public class TreeMarkovAnnotation {



    private static Tree<String> binarizeTree(Tree<String> tree) {
        String label = tree.getLabel();
        if (tree.isLeaf()) return new Tree<String>(label);
        if (tree.getChildren().size() == 1) { return new Tree<String>(label, Collections.singletonList(binarizeTree(tree.getChildren().get(0)))); }
        // otherwise, it's a binary-or-more local tree, so decompose it into a sequence of binary and unary trees.
        String intermediateLabel = "@" + label + "->";
        Tree<String> intermediateTree = binarizeTreeHelper(tree, 0, intermediateLabel);
        return new Tree<String>(label, intermediateTree.getChildren());
    }

    private static Tree<String> binarizeTreeHelper(Tree<String> tree, int numChildrenGenerated, String intermediateLabel) {
        Tree<String> leftTree = tree.getChildren().get(numChildrenGenerated);
        List<Tree<String>> children = new ArrayList<Tree<String>>();
        children.add(binarizeTree(leftTree));
        if (numChildrenGenerated < tree.getChildren().size() - 1) {
            Tree<String> rightTree = binarizeTreeHelper(tree, numChildrenGenerated + 1, intermediateLabel + "_" + leftTree.getLabel());
            children.add(rightTree);
        }
        return new Tree<String>(intermediateLabel, children);
    }

    public static Tree<String> unAnnotateTree(Tree<String> annotatedTree) {
        // Remove intermediate nodes (labels beginning with "@"
        // Remove all material on node labels which follow their base symbol (cuts anything after <,>,^,=,_ or ->)
        // Examples: a node with label @NP->DT_JJ will be spliced out, and a node with label NP^S will be reduced to NP
        Tree<String> debinarizedTree = Trees.spliceNodes(annotatedTree, new Filter<String>()
        {
            public boolean accept(String s) {
                return s.startsWith("@");
            }
        });
        Tree<String> unAnnotatedTree = (new Trees.LabelNormalizer()).transformTree(debinarizedTree);
        return unAnnotatedTree;
    }
}
