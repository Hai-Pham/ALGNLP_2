package edu.berkeley.nlp.assignments.parsing.student.util;

import java.util.ArrayList;
import java.util.List;

import edu.berkeley.nlp.assignments.parsing.BinaryRule;
import edu.berkeley.nlp.assignments.parsing.UnaryRule;
import edu.berkeley.nlp.ling.Tree;
import edu.berkeley.nlp.util.CollectionUtils;
import edu.berkeley.nlp.util.Counter;
import edu.berkeley.nlp.util.Indexer;

/**
 * Simple implementation of a PCFG grammar, offering the ability to look up
 * rules by their child symbols. Rule probability estimates are just relative
 * frequency estimates off of training trees.
 */
public class Grammar {

    Indexer<String> labelIndexer;

    List<BinaryRule>[] binaryRulesByLeftChild;
    List<BinaryRule>[] binaryRulesByRightChild;
    List<BinaryRule>[] binaryRulesByParent;
    List<BinaryRule> binaryRules = new ArrayList<BinaryRule>();

    List<UnaryRule>[] unaryRulesByChild;
    List<UnaryRule>[] unaryRulesByParent;
    List<UnaryRule> unaryRules = new ArrayList<UnaryRule>();

    public Indexer<String> getLabelIndexer() {
        return labelIndexer;
    }

    public List<BinaryRule> getBinaryRulesByLeftChild(int leftChildIdx) {
        return binaryRulesByLeftChild[leftChildIdx];
    }

    public List<BinaryRule> getBinaryRulesByRightChild(int rightChildIdx) {
        return binaryRulesByRightChild[rightChildIdx];
    }

    public List<BinaryRule> getBinaryRulesByParent(int parentIdx) {
        return binaryRulesByParent[parentIdx];
    }

    public List<BinaryRule> getBinaryRules() {
        return binaryRules;
    }

    public List<UnaryRule> getUnaryRulesByChild(int childIdx) {
        return unaryRulesByChild[childIdx];
    }

    public List<UnaryRule> getUnaryRulesByParent(int parentIdx) {
        return unaryRulesByParent[parentIdx];
    }

    public List<UnaryRule> getUnaryRules() {
        return unaryRules;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        List<String> ruleStrings = new ArrayList<String>();
        for (int parent = 0; parent < binaryRulesByParent.length; parent++) {
            for (BinaryRule binaryRule : getBinaryRulesByParent(parent)) {
                ruleStrings.add(binaryRule.toString(labelIndexer));
            }
        }
        for (int parent = 0; parent < unaryRulesByParent.length; parent++) {
            for (UnaryRule fastUnaryRule : getUnaryRulesByParent(parent)) {
                ruleStrings.add(fastUnaryRule.toString(labelIndexer));
            }
        }
        for (String ruleString : CollectionUtils.sort(ruleStrings)) {
            sb.append(ruleString);
            sb.append("\n");
        }
        return sb.toString();
    }

    private void addTreeLabels(Tree<String> tree) {
        if (!tree.isLeaf()) {
            labelIndexer.addAndGetIndex(tree.getLabel());
            if (!tree.getChildren().isEmpty()) {
                for (Tree<String> child : tree.getChildren()) {
                    addTreeLabels(child);
                }
            }
        }
    }

    private void addBinary(BinaryRule binaryRule) {
        binaryRules.add(binaryRule);
        binaryRulesByParent[binaryRule.getParent()].add(binaryRule);
        binaryRulesByLeftChild[binaryRule.getLeftChild()].add(binaryRule);
        binaryRulesByRightChild[binaryRule.getRightChild()].add(binaryRule);
    }

    private void addUnary(UnaryRule unaryRule) {
        unaryRules.add(unaryRule);
        unaryRulesByChild[unaryRule.getChild()].add(unaryRule);
        unaryRulesByParent[unaryRule.getParent()].add(unaryRule);
    }

    /**
     * Sets the weights so that scores of rules in the grammar are their log
     * probability of MLE estimates on the trees in trainTrees.
     *
     * @param trainTrees
     * @return
     */
    public static edu.berkeley.nlp.assignments.parsing.student.util.Grammar generativeGrammarFromTrees(List<Tree<String>> trainTrees) {
        return new edu.berkeley.nlp.assignments.parsing.student.util.Grammar(trainTrees);
    }

    private Grammar(List<Tree<String>> trainTrees) {
        this.labelIndexer = new Indexer<String>();
        for (Tree<String> trainTree : trainTrees) {
            addTreeLabels(trainTree);
        }
        this.binaryRulesByLeftChild = new List[labelIndexer.size()];
        this.binaryRulesByRightChild = new List[labelIndexer.size()];
        this.binaryRulesByParent = new List[labelIndexer.size()];
        this.unaryRulesByChild = new List[labelIndexer.size()];
        this.unaryRulesByParent = new List[labelIndexer.size()];
        for (int i = 0; i < labelIndexer.size(); i++) {
            this.binaryRulesByLeftChild[i] = new ArrayList<BinaryRule>();
            this.binaryRulesByRightChild[i] = new ArrayList<BinaryRule>();
            this.binaryRulesByParent[i] = new ArrayList<BinaryRule>();
            this.unaryRulesByChild[i] = new ArrayList<UnaryRule>();
            this.unaryRulesByParent[i] = new ArrayList<UnaryRule>();
        }
        Counter<UnaryRule> unaryRuleCounter = new Counter<UnaryRule>();
        Counter<BinaryRule> binaryRuleCounter = new Counter<BinaryRule>();
        Counter<Integer> symbolCounter = new Counter<Integer>();
        for (Tree<String> trainTree : trainTrees) {
            tallyTree(trainTree, symbolCounter, unaryRuleCounter, binaryRuleCounter);
        }
        for (UnaryRule unaryRule : unaryRuleCounter.keySet()) {
            double unaryProbability = unaryRuleCounter.getCount(unaryRule) / symbolCounter.getCount(unaryRule.getParent());
            unaryRule.setScore(Math.log(unaryProbability));
            addUnary(unaryRule);
        }
        for (BinaryRule binaryRule : binaryRuleCounter.keySet()) {
            double binaryProbability = binaryRuleCounter.getCount(binaryRule) / symbolCounter.getCount(binaryRule.getParent());
            binaryRule.setScore(Math.log(binaryProbability));
            addBinary(binaryRule);
        }
    }

    private void tallyTree(Tree<String> tree, Counter<Integer> symbolCounter, Counter<UnaryRule> unaryRuleCounter, Counter<BinaryRule> binaryRuleCounter) {
        if (tree.isLeaf()) return;
        if (tree.isPreTerminal()) return;
        if (tree.getChildren().size() == 1) {
            symbolCounter.incrementCount(labelIndexer.indexOf(tree.getLabel()), 1.0);
            unaryRuleCounter.incrementCount(makeUnaryRule(tree), 1.0);
        }
        if (tree.getChildren().size() == 2) {
            symbolCounter.incrementCount(labelIndexer.indexOf(tree.getLabel()), 1.0);
            binaryRuleCounter.incrementCount(makeBinaryRule(tree), 1.0);
        }
        if (tree.getChildren().size() < 1 || tree.getChildren().size() > 2) { throw new RuntimeException("Attempted to construct a Grammar with an illegal tree (unbinarized?): " + tree); }
        for (Tree<String> child : tree.getChildren()) {
            tallyTree(child, symbolCounter, unaryRuleCounter, binaryRuleCounter);
        }
    }

    private UnaryRule makeUnaryRule(Tree<String> tree) {
        return new UnaryRule(labelIndexer.indexOf(tree.getLabel()), labelIndexer.indexOf(tree.getChildren().get(0).getLabel()));
    }

    private BinaryRule makeBinaryRule(Tree<String> tree) {
        return new BinaryRule(labelIndexer.indexOf(tree.getLabel()), labelIndexer.indexOf(tree.getChildren().get(0).getLabel()), labelIndexer.indexOf(tree.getChildren().get(1).getLabel()));
    }
}