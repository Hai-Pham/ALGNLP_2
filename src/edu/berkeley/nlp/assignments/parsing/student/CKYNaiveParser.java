package edu.berkeley.nlp.assignments.parsing.student;

import edu.berkeley.nlp.assignments.parsing.*;
import edu.berkeley.nlp.assignments.parsing.student.util.Lexicon;
import edu.berkeley.nlp.assignments.parsing.student.util.Grammar;
import edu.berkeley.nlp.assignments.parsing.student.util.TreeMarkovAnnotation;
import edu.berkeley.nlp.assignments.parsing.student.util.Triple;
import edu.berkeley.nlp.ling.Tree;
import edu.berkeley.nlp.util.CounterMap;
import edu.berkeley.nlp.util.Indexer;

import java.util.*;


public class CKYNaiveParser implements Parser
{
    public static class CKYNaiveParserFactory implements ParserFactory {

        public Parser getParser(List<Tree<String>> trainTrees) {
            return new edu.berkeley.nlp.assignments.parsing.student.CKYNaiveParser(trainTrees);
        }
    }

    // List of score and back pointer tables
    // Key of all internal HashMaps are the index of Non-Terminal Symbols
    // (managed by grammar's labelIndexer)
    private static ArrayList<ArrayList<HashMap<Integer, Double>>> unaryScore, binaryScore, score;
    private static ArrayList<ArrayList<HashMap<Integer, Integer>>> uniBackPointer;
    private static ArrayList<ArrayList<HashMap<Integer, Triple>>> biBackPointer;
    private static UnaryClosure unaryClosure;

    Lexicon lexicon;
    edu.berkeley.nlp.assignments.parsing.student.util.Grammar grammar;
    int numNonTerminals;


    // Markovization constant
    private final int vOrder = 2;
    private final int hOrder = 2;
    /**
     * Given a sentence, yield a best parse in terms of Tree data structure
     * The most important method - will be called by a Test Case
     * @param sentence
     * @return Tree<String> of best parse
     */
    public Tree<String> getBestParse(List<String> sentence) {
        cky(sentence);
        Tree<String> annotatedBestParse = createCKYParsedTree(sentence, 0, false, 0, sentence.size());
        return TreeMarkovAnnotation.unAnnotateTree(annotatedBestParse);
    }

    public CKYNaiveParser(List<Tree<String>> trainTrees) {
        System.out.print("Annotating / binarizing training trees ... ");
        List<Tree<String>> annotatedTrainTrees = annotateTrees(trainTrees);
        System.out.println("done.");
        System.out.print("Building grammar ... ");
        grammar = Grammar.generativeGrammarFromTrees(annotatedTrainTrees);
        System.out.println("done. (" + grammar.getLabelIndexer().size() + " states)");
        System.out.println("Building lexicon...");
        lexicon = new Lexicon(annotatedTrainTrees);

        // init scores and back pointers tables
        numNonTerminals = grammar.getLabelIndexer().size();

        System.out.println("There are " + numNonTerminals + " non terminals after training");
        System.out.println("done. with lexicon size of " + lexicon.getAllTags().size());

        System.out.println("Init unary closure...");
        initUnaryClosureRules();
    }

    /**
     * Some console debugging for grammar and lexicon
     */
    public void debugLexiconToConsole(){
        CounterMap<String, String> lexiconMap = lexicon.getLexicon();
//        System.out.println(lexiconMap);
        System.out.println(lexicon);

        // Print out tags only
//        Set<String> tags = lexicon.getAllTags();
//        for (String s: tags)
//            System.out.print(s + " ");
//        System.out.println();
    }
    public void debugGrammarToConsole() {
        printUnaryRules();
        printBinaryRules();
    }
    public void printUnaryRules() {
        List<UnaryRule> unaryRules = grammar.getUnaryRules();
        System.out.println("Unary rules are: \n");
        System.out.println(unaryRules);

        Indexer<String> labelIndexer = grammar.getLabelIndexer();
        for (UnaryRule unaryRule: unaryRules) {
            String parent = labelIndexer.get(unaryRule.getParent());
            String child = labelIndexer.get(unaryRule.getChild());
//            System.out.println(unaryRule.toString(labelIndexer));
            System.out.println(parent + "(" + labelIndexer.indexOf(parent) + ")" +
                    " ==> " + child + "(" + labelIndexer.indexOf(child) + ")");
        }
    }
    public void printBinaryRules() {
        List<BinaryRule> binaryRules = grammar.getBinaryRules();
        System.out.println("Binary rules are: \n");
        System.out.println(binaryRules);
    }
    public void debugScoreTablesToConsole(ArrayList<ArrayList<HashMap<Integer, Double>>> score){
        System.out.println("SCORE Table");
        int c=0;
        for (List<HashMap<Integer, Double>>  listScoreMap: score) {
            System.out.println("*** Level " + c++);
            int cc = 0;
            for (HashMap<Integer, Double> scoreMap : listScoreMap)
                System.out.println("=== Sublevel =" + cc++  + "==="  + scoreMap);
        }
        System.out.println();
    }
    public void debugBackPointerTablesToConsole() {
        System.out.println("Unary Back Pointer Table:");
        int c=0;
        for (List<HashMap<Integer, Integer>> listUnaryBackPtr: uniBackPointer) {
            System.out.println("*** Level " + c++);
            int cc = 0;
            for (HashMap<Integer, Integer> unaryBackPtr : listUnaryBackPtr)
                System.out.println("=== Sublevel =" + cc++  + "===" + unaryBackPtr);
        }
        System.out.println();
        System.out.println("Binary Back Pointer Table:");
        c=0;
        for (List<HashMap<Integer, Triple>> listBinaryBackPtr: biBackPointer) {
            System.out.println("*** Level " + c++);
            int cc = 0;
            for (HashMap<Integer, Triple> binaryBackPtr : listBinaryBackPtr)
                System.out.println("=== Sublevel =" + cc++  + "===" + binaryBackPtr);
        }
        System.out.println();
    }

    private List<Tree<String>> annotateTrees(List<Tree<String>> trees) {
        List<Tree<String>> annotatedTrees = new ArrayList<>();
        for (Tree<String> tree : trees) {
            annotatedTrees.add(TreeMarkovAnnotation.annotateTree(tree, vOrder, hOrder));
        }
        return annotatedTrees;
    }

    public Lexicon getLexicon() {
        return lexicon;
    }
    public Grammar getGrammar() {
        return grammar;
    }

    public static ArrayList<ArrayList<HashMap<Integer, Double>>> getUnaryScore() {
        return unaryScore;
    }

    public static ArrayList<ArrayList<HashMap<Integer, Double>>> getBinaryScore() {
        return binaryScore;
    }

    public static ArrayList<ArrayList<HashMap<Integer, Double>>> getScore() {
        return score;
    }

    public static ArrayList<ArrayList<HashMap<Integer, Integer>>> getUniBackPointer() {
        return uniBackPointer;
    }

    public static ArrayList<ArrayList<HashMap<Integer, Triple>>> getBiBackPointer() {
        return biBackPointer;
    }

    /**
     * Init scores and back pointers tables for CKY
     * TODO: change back to private after debugging
     * @param sentence: input of CKY
     */
    public void initScoreAndBackPointerTables(List<String> sentence) {
//        System.out.println("Initializing score and back pointer tables for " + sentence);
        score = new ArrayList<>();
        unaryScore = new ArrayList<>();
        binaryScore = new ArrayList<>();
        uniBackPointer = new ArrayList<>();
        biBackPointer = new ArrayList<>();
        int n = sentence.size();

        for (int i=0; i<n+1; i++) {
            ArrayList<HashMap<Integer, Double>> scoreList = new ArrayList<>();
            ArrayList<HashMap<Integer, Double>> unaryScoreList = new ArrayList<>();
            ArrayList<HashMap<Integer, Double>> binaryScoreList = new ArrayList<>();
            ArrayList<HashMap<Integer, Integer>> uniList = new ArrayList<>();
            ArrayList<HashMap<Integer, Triple>> biList = new ArrayList<>();

            for (int j = 0; j < n + 1; j++) {
                HashMap<Integer, Double> scoreMap = new HashMap<>();
                HashMap<Integer, Double> unaryScoreMap = new HashMap<>();
                HashMap<Integer, Double> binaryScoreMap = new HashMap<>();
                HashMap<Integer, Integer> uniBackPtrMap = new HashMap<>();
                HashMap<Integer, Triple> biBackPtrMap = new HashMap<>();
                // build n ArrayList of HashMap\
                scoreList.add(scoreMap);
                unaryScoreList.add(unaryScoreMap);
                binaryScoreList.add(binaryScoreMap);
                uniList.add(uniBackPtrMap);
                biList.add(biBackPtrMap);
            }
            // build n ArrrayList of ArrayList
            score.add(scoreList);
            unaryScore.add(unaryScoreList);
            binaryScore.add(binaryScoreList);
            uniBackPointer.add(uniList);
            biBackPointer.add(biList);
        }
    }

    /**
     * Initialize the Unary Closure Rules, this should be done only ...
     * ... after all grammar and lexicon are done
     * This will be called in the constructor
     */
    public void initUnaryClosureRules(){
        Indexer<String> labelIndexer = grammar.getLabelIndexer();
        unaryClosure = new UnaryClosure(labelIndexer, grammar.getUnaryRules());
    }

    /**
     * CKY implememtation, given grammar and lexicon
     * This algorithm interleaves binary and unary cases, facilitating the building parse tree later
     * @param sentence as a list of string
     * will update the private fields of scores and back pointers
     */
    public void cky(List<String> sentence) {
//        System.out.println("\n\n\n\n\n====================RUNNING CKY FOR  " + sentence + "===================");

        // Init essential data structures
        initScoreAndBackPointerTables(sentence);
        Indexer<String> labelIndexer = grammar.getLabelIndexer();
        int n = sentence.size();

        for (int i = 0; i < sentence.size(); i++) {
//            System.out.println("--BASE CASE---");
//            int i = sentence.indexOf(w); // stupidity here !!!
            // process all pre-terminals
            for (String tag: lexicon.getAllTags()) {
                // check whether A -> s is in lexicon
                if (lexicon.getLexicon().getCount(sentence.get(i), tag) > 0) {
//                    System.out.format("Updating entry[%d][%d] with k = %d, v=%.2f\n", i, i+1, labelIndexer.indexOf(tag), lexicon.scoreTagging(sentence.get(i), tag));
//                    unaryScore.get(i).get(i+1).put(labelIndexer.indexOf(tag), lexicon.scoreTagging(sentence.get(i), tag));
                    score.get(i).get(i+1).put(labelIndexer.indexOf(tag), lexicon.scoreTagging(sentence.get(i), tag));
                }
            }

            // get unary score and back pointer tables for processing extended unaries
            HashMap<Integer, Double> scoreMap = score.get(i).get(i+1);
//            HashMap<Integer, Double> unaryScoreMap = unaryScore.get(i).get(i+1);
            HashMap<Integer, Integer> unaryBackPtrMap = uniBackPointer.get(i).get(i+1);

            // handle unaries
            handleUnaries(i, i+1, scoreMap, unaryBackPtrMap);
//            handleUnaries(i, i+1, unaryScoreMap, unaryBackPtrMap);
        }

        // MAIN PROCESSING
        // Alternating between binaries and unaries
        for (int span = 2; span <= n; span++) {
            for (int begin = 0; begin <= (n - span); begin++) {
                int end = begin + span;
                //handle unaries
                HashMap<Integer, Double> scoreMap = score.get(begin).get(end);
                HashMap<Integer, Double> unaryScoreMap = unaryScore.get(begin).get(end);
//                HashMap<Integer, Double> binaryScoreMap = binaryScore.get(begin).get(end);
                HashMap<Integer, Integer> uniBackPtrMap = uniBackPointer.get(begin).get(end);

                for (int split= begin + 1; split <= end - 1; split++) {
                    //handles binary
//                    System.out.println("***\n\n\nBINARY CASE***");
//                    System.out.println("\n##### AT SPLIT = " + split + " of [" + begin + "," + end + "] #####");

                    HashMap<Integer, Double> leftBinaryScores = score.get(begin).get(split);
                    HashMap<Integer, Double> rightBinaryScores = score.get(split).get(end);
//                    HashMap<Integer, Double> leftBinaryScores = unaryScore.get(begin).get(split);
//                    HashMap<Integer, Double> rightBinaryScores = unaryScore.get(split).get(end);

                    for (Integer B: leftBinaryScores.keySet()) {
                        for (BinaryRule AtoBC: grammar.getBinaryRulesByLeftChild(B)) {
                            int A = AtoBC.getParent();
                            //DEBUG
//                            System.out.println(" A->BC rule: " + AtoBC + "(" + labelIndexer.get(A)
//                                    + " -> " + labelIndexer.get(AtoBC.getLeftChild()) + "-" +
//                                    labelIndexer.get(AtoBC.getRightChild()) + "), p=" + AtoBC.getScore());
                            int C = AtoBC.getRightChild();

                            // check whether right child's score exists
                            if (! rightBinaryScores.containsKey(C)) {
                                continue;
                            }

                            double prob = leftBinaryScores.get(B) + rightBinaryScores.get(C) + AtoBC.getScore();
                            HashMap<Integer, Double> binaryScoreMap = score.get(begin).get(end);
                            if ( !(binaryScoreMap.containsKey(A)) || (prob > binaryScoreMap.get(A)) ) {
//                                DEBUG
//                                System.out.println(" A->BC rule: " + AtoBC + "(" + labelIndexer.get(A)
//                                        + " -> "
// + labelIndexer.get(AtoBC.getLeftChild()) + "-" +
//                                        labelIndexer.get(AtoBC.getRightChild()) + "), p=" + AtoBC.getScore());

                                // update score and back pointer
                                binaryScoreMap.put(A, prob);
//                                System.out.println("    ----------<<<<<Updating BIscores entry [" + begin + "][" + end + "] " +
//                                        "with k=" + A + " v=" + prob);
                                biBackPointer.get(begin).get(end).put(A, new Triple<>(split, B, C));
//                                System.out.println("    ----------Updating BI Back pointer entry [" + begin + "][" + end + "] " +
//                                        "with k=" + A + " v=" + split + "," + B + "," + C);
                            }
                        }
                    }
//                System.out.println("***\nUNARY CASE***");
                    handleUnaries(begin, end, scoreMap, uniBackPtrMap);
//                    handleUnaries(begin, end, unaryScoreMap, uniBackPtrMap);
                }
            }
        }
    }



    /**
     * Handle Unary Case in CKY
     * TODO: switch back to private after debugging
     * TODO: check whether need to check base case:  end = start + 1
     * @param begin index
     * @param end index
     */
    public void handleUnaries(int begin, int end, HashMap<Integer, Double> unaryScoreMap, HashMap<Integer, Integer> unaryBackPtrMap) {
//        System.out.println("--Handling Unaries...");
//        Indexer<String> labelIndexer = grammar.getLabelIndexer();

//        System.out.println("UnaryScoreMap is: " + unaryScoreMap);
//        System.out.println("UnaryBackPtrMap is " + unaryBackPtrMap);

        boolean added = true;
        while (added) {
            added = false;
//            System.out.println("----------Processing this key set: " + unaryScoreMap.keySet());
            // TODO: scan only valid tags B: IMPORTANT
            Set<Integer> keySet = new HashSet<>(unaryScoreMap.keySet()); // deep copy to avoid exception
            for (int B: keySet) {
                // get all A s.t. A -> B is a unary closure rule
                for (UnaryRule AtoB: unaryClosure.getClosedUnaryRulesByChild(B)) {
//                    System.out.format("----------------Evaluating %s with p=%.2f\n", AtoB, AtoB.getScore());
                    double p_A_to_B = AtoB.getScore();
                    double prob = p_A_to_B + unaryScoreMap.get(B);
                    int A = AtoB.getParent();
                    if ( (! unaryScoreMap.containsKey(A)) || (prob > unaryScoreMap.get(A)) ) {
                        // DEBUG
//                        System.out.println(" A->B unary closure rule: " + AtoB +
//                                "(" + labelIndexer.get(AtoB.getParent()) + " => " +
//                                labelIndexer.get(AtoB.getChild()) + "), p=" + AtoB.getScore());
                        unaryScoreMap.put(A, prob);
//                        System.out.println("    ----------(Handle)Updating UNIscores entry [" + begin + "][" + end + "] " +
//                                "with k=" + A + " v=" + prob);
                        uniBackPointer.get(begin).get(end).put(A, B);
//                        System.out.println("    ----------Updating UNI Backpointer entry [" + begin + "][" + end + "] " +
//                                "with k=" + A + " v=" + B);
                        added = true;
                    }
                }
            }
        }
    }

    /**
     * Create a parsed tree after CKY Parsing recursively
     */
    public Tree<String> createCKYParsedTree(List<String> sentence, int parent, boolean isBinaryTurn,
                                    int start, int end) {
//        System.out.println("\nBuiding the parsed tree...");
        Indexer<String> labelIndexer = grammar.getLabelIndexer();

        // base case
        if (end == start + 1) {
//            System.out.println("Base case with start=" + start + " and end=" + end + " and parent=" + parent);
            // terminals (words)
            if (uniBackPointer.get(start).get(end).get(parent) == null)
                // parent -> word
                return new Tree<>(labelIndexer.get(parent), Arrays.asList(new Tree<>(sentence.get(start))));
            else {
                // parent -> tag -> word
                int tag = uniBackPointer.get(start).get(end).get(parent);
                Tree<String> tagToWordTree = new Tree<>(labelIndexer.get(tag),
                        Arrays.asList(new Tree<>(sentence.get(start))));
                return new Tree<>(labelIndexer.get(parent), Arrays.asList(tagToWordTree));
            }
        }

        Tree<String> masterTree = new Tree<>(labelIndexer.get(parent));
        // unary
        if (! isBinaryTurn) {
//            System.out.println("Unary case with start=" + start + " and end=" + end + " and parent=" + parent);
            Tree<String> unaryTree = null;
            // TODO: evaluating this whether true or not
            if (!uniBackPointer.get(start).get(end).containsKey(parent))
                unaryTree = createCKYParsedTree(sentence, parent, true, start, end);
            else {
                // get the back point of parent
                int child = uniBackPointer.get(start).get(end).get(parent);
                unaryTree = createCKYParsedTree(sentence, child, true, start, end);
            }
            masterTree.setChildren(Arrays.asList(unaryTree));
        }
        else {
        // binary
//            System.out.println("Binary case with start=" + start + " and end=" + end + " and parent=" + parent);
            Triple<Integer, Integer, Integer> triple = biBackPointer.get(start).get(end).get(parent);

            // failed to parse, return a random tree
            if (triple == null)
                return new Tree<>("ROOT", Collections.singletonList(new Tree<>("JUNK")));

            int leftChild = triple.begin;
            int rightChild = triple.end;
            int split = triple.split;
            Tree<String> leftChildTree = createCKYParsedTree(sentence, leftChild, false, start, split);
            Tree<String> rightChildTree = createCKYParsedTree(sentence, rightChild, false, split, end);

            masterTree.setChildren(Arrays.asList(leftChildTree, rightChildTree));
        }

        return masterTree;
    }
}