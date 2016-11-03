package edu.berkeley.nlp.assignments.parsing.student.util;

import edu.berkeley.nlp.io.PennTreebankReader;
import edu.berkeley.nlp.ling.Tree;
import edu.berkeley.nlp.ling.Trees;
import edu.berkeley.nlp.util.Filter;

import java.util.*;

/**
 * Created by Gorilla on 10/23/2016.
 */
public class TreeMarkovAnnotation {


    public static Tree<String> annotateTree(Tree<String> unAnnotatedTree, int verticalOrder, int horizontalOrder) {
        Tree<String> vTree = verticallyMarkovize(unAnnotatedTree, new ArrayList<String>(), verticalOrder);
        return binarizeTree(vTree, horizontalOrder);
    }

    /**
     * Vertically markovize the tree
     * The format is as follows: child ^ parent (order = 1),
     * ... child ^ grandparent ^ parent (order = 2), and so on
     * @param tree
     * @param parents
     * @param verticalOrder
     * @return a vertically markovized tree
     */
    private static Tree<String> verticallyMarkovize(Tree<String> tree, ArrayList<String> parents, int verticalOrder) {
        // index from 1 not from 0 for vertical markovization
        // i.e. h = 1: nothing to record in terms of parents
        verticalOrder--;

        // base case
        if (tree.isLeaf()) {
            return tree;
        }

        String label = tree.getLabel();
        for (String parent : parents)
            label += "^" + parent;
        ArrayList<String> newParents = new ArrayList<>(parents);


        // forget parent(s) based on order
        newParents.add(tree.getLabel());
        if (newParents.size() > verticalOrder)
            newParents.remove(0);
        // reverse parents
        Collections.reverse(newParents);


        ArrayList<Tree<String>> children = new ArrayList<>();
        // Recursion
        for (Tree<String> child : tree.getChildren()) {
            Tree<String> childTree = verticallyMarkovize(child, newParents, verticalOrder);
            children.add(childTree);
        }

        Tree<String> vTree = new Tree<>(label, children);

        return vTree;
    }


    /**
     * Input a vertically processed tree, do a binarization and horizontal at once
     * @param tree
     * @param horizontalOrder
     * @return
     */
    private static Tree<String> binarizeTree(Tree<String> tree, int horizontalOrder) {
        String label = tree.getLabel();
        if (tree.isLeaf()) {
            return new Tree<>(label);
        }
        if (tree.getChildren().size() == 1) {
            return new Tree<>(label, Collections.singletonList(binarizeTree(tree.getChildren().get(0), horizontalOrder)));
        }

        // new updated code
        if (tree.getChildren().size() == 2) {
            return new Tree(label, Arrays.asList(
                    binarizeTree(tree.getChildren().get(0), horizontalOrder),
                    binarizeTree(tree.getChildren().get(1), horizontalOrder)
            ));
        }
        // otherwise, it's a binary-or-more local tree, so decompose it into a sequence of binary and unary trees.
        String intermediateLabel = "@" + label + "->";
        Tree<String> intermediateTree = binarizeTreeHelper(tree, 0, intermediateLabel, horizontalOrder);
        return new Tree<>(label, intermediateTree.getChildren());
    }

    /**
     * Deal with the case where a parent has more than 1 child (needs horizontal markovization)
     * @param tree
     * @param numChildrenGenerated
     * @param intermediateLabel
     * @param horizontalOrder: horizontal order
     * @return
     */
    private static Tree<String> binarizeTreeHelper(Tree<String> tree, int numChildrenGenerated, String intermediateLabel,
                                                   int horizontalOrder) {

        Tree<String> leftTree = tree.getChildren().get(numChildrenGenerated);
        List<Tree<String>> children = new ArrayList<Tree<String>>();
        children.add(binarizeTree(leftTree, horizontalOrder));
        if (numChildrenGenerated < tree.getChildren().size() - 1) {
            Tree<String> rightTree = binarizeTreeHelper(tree, numChildrenGenerated + 1, intermediateLabel + "_" + leftTree.getLabel(), horizontalOrder);

            // process horizontal markovization
            String rightTreeRefinedLabel = processRightChildLabelHorizontally(rightTree.getLabel(), horizontalOrder);
            rightTree.setLabel(rightTreeRefinedLabel);
            children.add(rightTree);
        }
        return new Tree<>(intermediateLabel, children);
    }

    /**
     * Get the last "order" lalels only from root label, forget everything else
     * @param rightTreeLabel
     * @param order
     * @return
     */
    private static String processRightChildLabelHorizontally(String rightTreeLabel, int order) {
//        System.out.format("=================Processing string %s\n", rightTreeLabel);

        String[] fields = rightTreeLabel.split("_");
        int l = fields.length;

        String result = fields[0];

        if (order == -1) {
            for ( int i = 1; i < fields.length; i++) {
                result += ("_" + getRidOfParentNames(fields[i]));
            }
            return result;
        }

        if (l > 2) {
            result += "...";
            for (int i = fields.length - order; i < fields.length; i++) {
                result += ("_" + getRidOfParentNames(fields[i]));
            }
        } else if (l == 2) {
            result += ("_" + getRidOfParentNames(fields[1]));
        }
        return result;
    }

    /**
     * E.g. _JJ^ADJP^ROOT => _JJ only
     * @param s
     * @return result a processed string
     */
    private static String getRidOfParentNames(String s) {
        String[] fields = s.split("\\^");
        String result = fields[0];
        return result;
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

    private static List<Tree<String>> readTrees(String basePath, int low, int high, int maxLength) {
        Collection<Tree<String>> trees = PennTreebankReader.readTrees(basePath, low, high);
        // normalize trees
        Trees.TreeTransformer<String> treeTransformer = new Trees.StandardTreeNormalizer();
        List<Tree<String>> normalizedTreeList = new ArrayList<Tree<String>>();
        for (Tree<String> tree : trees) {
            Tree<String> normalizedTree = treeTransformer.transformTree(tree);
            if (normalizedTree.getYield().size() > maxLength) continue;
            //      System.out.println(Trees.PennTreeRenderer.render(normalizedTree));
            normalizedTreeList.add(normalizedTree);
        }
        return normalizedTreeList;
    }

    // TEST
    public static void main(String[] args) {
        String basePath = ".";
        int start = 446;
        int end = 446;
        int maxTrainLength = 8;

        int vOrder = 2;
        int hOrder = 1;

        System.out.print("Loading training trees  ... ");
        List<Tree<String>> trainTrees = readTrees(basePath, start, end, maxTrainLength);
        System.out.println("done. (" + trainTrees.size() + " trees)");

        System.out.println(Trees.PennTreeRenderer.render(trainTrees.get(0)));

        // Naive annotation and binarization
//        Tree<String> vTree = binarizeTree(trainTrees.get(0));
//        System.out.println(Trees.PennTreeRenderer.render(vTree));
//        Tree<String> unannotatedTree = unAnnotateTree(vTree);
//        System.out.println(Trees.PennTreeRenderer.render(unannotatedTree));

        Tree<String> vTree = verticallyMarkovize(trainTrees.get(2), new ArrayList<String>(), vOrder);
        System.out.println(Trees.PennTreeRenderer.render(vTree));
        Tree<String> binarizedTree = binarizeTree(vTree, hOrder);
        System.out.println(Trees.PennTreeRenderer.render(binarizedTree));
        Tree<String> unannotatedTree = unAnnotateTree(binarizedTree);
        System.out.println(Trees.PennTreeRenderer.render(binarizedTree));

//        String s = "@S->_CC_NP_VP";
//        System.out.println(processRightChildLabelHorizontally(s, 1));
        ArrayList<String> ss = new ArrayList<>(Arrays.asList("This", "is"));
        Collections.reverse(ss);
        System.out.println(ss);
    }
}
