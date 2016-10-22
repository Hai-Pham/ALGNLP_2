package edu.berkeley.nlp.assignments.parsing.student;

import edu.berkeley.nlp.assignments.parsing.*;
import edu.berkeley.nlp.assignments.parsing.student.util.Lexicon;
import edu.berkeley.nlp.assignments.parsing.student.util.Grammar;
import edu.berkeley.nlp.assignments.parsing.student.util.Triple;
import edu.berkeley.nlp.ling.Tree;
import edu.berkeley.nlp.math.DoubleArrays;
import edu.berkeley.nlp.util.CounterMap;
import edu.berkeley.nlp.util.Indexer;
import edu.berkeley.nlp.util.Interner;
import sun.rmi.server.InactiveGroupException;

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

    public static UnaryClosure getUnaryClosure() {
        return unaryClosure;
    }

    CounterMap<List<String>, Tree<String>> knownParses;
    CounterMap<Integer, String> spanToCategories;

    Lexicon lexicon;
    edu.berkeley.nlp.assignments.parsing.student.util.Grammar grammar;
    int numNonTerminals;

    /**
     * Given a sentence, yield a best parse in terms of Tree data structure
     * The most important method - will be called by a Test Case
     * @param sentence
     * @return Tree<String> of best parse
     */
    public Tree<String> getBestParse(List<String> sentence) {
        cky(sentence);
        Tree<String> annotatedBestParse = createCKYParsedTree(sentence, 0, false, 0, sentence.size());

        return TreeAnnotations.unAnnotateTree(annotatedBestParse);
    }

    /**
     * Finalizing the tree by adding the "ROOT" element on top of it
     * @param tree
     * @return a tree with ROOT on top
     */
    private Tree<String> addRoot(Tree<String> tree) {
        return new Tree<String>("ROOT", Collections.singletonList(tree));
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
    public void debugScoreTablesToConsole(){
//        System.out.println("Unary Table:");
//        int c=0;
//        for (List<HashMap<Integer, Double>> listUnaryMap: unaryScore) {
//            System.out.println("*** Level " + c++);
//            int cc = 0;
//            for (HashMap<Integer, Double> unaryMap : listUnaryMap)
//                System.out.println("=== Sublevel =" + cc++  + "===" + unaryMap);
//        }
//        System.out.println();
//
//        System.out.println("Binary Table");
//        c=0;
//        for (List<HashMap<Integer, Double>>  listBinaryMap: binaryScore) {
//            System.out.println("*** Level " + c++);
//            int cc = 0;
//            for (HashMap<Integer, Double> binaryMap : listBinaryMap)
//                System.out.println("=== Sublevel =" + cc++  + "==="  + binaryMap);
//        }
//        System.out.println();


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
        List<Tree<String>> annotatedTrees = new ArrayList<Tree<String>>();
        for (Tree<String> tree : trees) {
            annotatedTrees.add(TreeAnnotations.annotateTreeLosslessBinarization(tree));
        }
        return annotatedTrees;
    }

    public Lexicon getLexicon() {
        return lexicon;
    }
    public Grammar getGrammar() {
        return grammar;
    }

    /**
     * Init scores and back pointers tables for CKY
     * TODO: change back to private after debugging
     * @param sentence: input of CKY
     */
    public void initScoreAndBackPointerTables(List<String> sentence) {
        System.out.println("Initializing score and back pointer tables for " + sentence);
        score = new ArrayList<>();
//        unaryScore = new ArrayList<>();
//        binaryScore = new ArrayList<>();
        uniBackPointer = new ArrayList<>();
        biBackPointer = new ArrayList<>();
        int n = sentence.size();

        for (int i=0; i<n+1; i++) {
            ArrayList<HashMap<Integer, Double>> scoreList = new ArrayList<>();
//            ArrayList<HashMap<Integer, Double>> unaryList = new ArrayList<>();
//            ArrayList<HashMap<Integer, Double>> binaryList = new ArrayList<>();
            ArrayList<HashMap<Integer, Integer>> uniList = new ArrayList<>();
            ArrayList<HashMap<Integer, Triple>> biList = new ArrayList<>();

            for (int j = 0; j < n + 1; j++) {
                HashMap<Integer, Double> scoreMap = new HashMap<>();
//                HashMap<Integer, Double> unaryMap = new HashMap<>();
//                HashMap<Integer, Double> binaryMap = new HashMap<>();
                HashMap<Integer, Integer> uniBackPtrMap = new HashMap<>();
                HashMap<Integer, Triple> biBackPtrMap = new HashMap<>();

//                for (int s = 0; s < numNonTerminals; s++) {
//                    unaryMap.put(s, Double.NEGATIVE_INFINITY);
//                    binaryMap.put(s, Double.NEGATIVE_INFINITY);
//                    uniBackPtrMap.put(s, null);
//                    biBackPtrMap.put(s, null);
//                }
                // build n ArrayList of HashMap
                scoreList.add(scoreMap);
//                unaryList.add(unaryMap);
//                binaryList.add(binaryMap);
                uniList.add(uniBackPtrMap);
                biList.add(biBackPtrMap);
            }
            // build n ArrrayList of ArrayList
            score.add(scoreList);
//            unaryScore.add(unaryList);
//            binaryScore.add(binaryList);
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
        System.out.println("\n\n\n\n\n====================RUNNING CKY FOR  " + sentence + "===================");

        // Init essential data structures
        initScoreAndBackPointerTables(sentence);
        Indexer<String> labelIndexer = grammar.getLabelIndexer();
        int n = sentence.size();

        for (String w: sentence) {
            System.out.println("--BASE CASE---");
//            ArrayList<Integer> validTags = new ArrayList<>(); // to deal with lexicon unlisted cases
            int i = sentence.indexOf(w);
            // process all pre-terminals
            for (String tag: lexicon.getAllTags()) {
                // check whether A -> s is in lexicon
                if (lexicon.getLexicon().getCount(w, tag) > 0) {
                    score.get(i).get(i+1).put(labelIndexer.indexOf(tag), lexicon.scoreTagging(w, tag));
//                    unaryScore.get(i).get(i+1).put(labelIndexer.indexOf(tag), lexicon.scoreTagging(w, tag));

                    System.out.println("----------Base case: Updating UNIscores entry [" + i + "][" + (i+1) + "] with k=" + labelIndexer.indexOf(tag) + " v=" + lexicon.scoreTagging(w, tag));
                    // update valid tags list
//                    validTags.add(labelIndexer.indexOf(tag));
                }
            }

            // get unary score and back pointer tables for processing extended unaries
            HashMap<Integer, Double> scoreMap = score.get(i).get(i+1);
//            HashMap<Integer, Double> unaryScoreMap = unaryScore.get(i).get(i+1);
            HashMap<Integer, Integer> unaryBackPtrMap = uniBackPointer.get(i).get(i+1);


//            // Handling unlisted unary rules
////            System.out.println("Valid tags are: " + validTags);
//            System.out.println("Now deadling with lexicon unlisted unary rules... ");
//            // make use of Unary Closure, init this static var for every sentence, only once
//            boolean added = true;
//            while (added == true) {
//                added = false;
//                for (String tag: lexicon.getAllTags()) {
//                    int validTag = labelIndexer.indexOf(tag);
//                    // find all the rules A -> validTag
//                    for (UnaryRule AtoValidTag : unaryClosure.getClosedUnaryRulesByChild(validTag)) {
//                        int A = AtoValidTag.getParent();
//                        //update tables and back pointers
//                        //                    if (A != validTag) {
//                        System.out.println("    There is the valid unary unlisted rule: " + AtoValidTag);
//                        double p_A_to_valiTag = AtoValidTag.getScore();
//                        double p_validTag_to_word_max = unaryScore.get(i).get(i + 1).get(validTag);
//                        System.out.println("    p(A->tag)=" + p_A_to_valiTag + " max p(tag->w)=" + p_validTag_to_word_max + ", p(A)=" + unaryScore.get(i).get(i + 1).get(A));
//
//                        // update to score and back pointers
//                        double prob = p_A_to_valiTag + p_validTag_to_word_max;
//                        HashMap<Integer, Double> unaryScoreMap = unaryScore.get(i).get(i+1);
//                        if ( (! unaryScoreMap.containsKey(A)) || (prob > unaryScoreMap.get(A)) ) {
//                            unaryScore.get(i).get(i + 1).put(A, prob);
//                            System.out.println("    ----------Updating UNIscores entry [" + i + "][" + (i + 1) + "] " +
//                                    "with k=" + A + " v=" + prob + "replacing " + unaryScore.get(i).get(i + 1).get(A));
//                            uniBackPointer.get(i).get(i + 1).put(A, validTag);
//                            System.out.println("    ----------Updating UNI Backpointer entry [" + i + "][" + (i + 1) + "] " +
//                                    "with k=" + A + " v=" + validTag);
//                            added = true;
//                        }
//                        //                    }
//                    }
//                }
//            }
            // END of Handling unlisted unary rules

            // handle unaries
            handleUnaries(i, i+1, scoreMap, unaryBackPtrMap);
//            unaryScore.get(i).set(i+1, scoreMap);

//            handleUnaries(i, i+1, unaryScoreMap, unaryBackPtrMap);
//            unaryScore.get(i).set(i+1, unaryScoreMap);
//            uniBackPointer.get(i).set(i+1, unaryBackPtrMap);
        }

        // MAIN PROCESSING
        // Alternating between binaries and unaries

        for (int span = 2; span <= n; span++) {
            for (int begin = 0; begin <= (n - span); begin++) {
                int end = begin + span;
                for (int split= begin + 1; split <= end - 1; split++) {
                    //handles binary
                    System.out.println("***\n\n\nBINARY CASE***");
                    System.out.println("\n##### AT SPLIT = " + split + " of [" + begin + "," + end + "] #####");

//                    HashMap<Integer, Double> leftBinaryScores = binaryScore.get(begin).get(split);
//                    HashMap<Integer, Double> rightBinaryScores = binaryScore.get(split).get(end);
//                    HashMap<Integer, Double> leftBinaryScores = unaryScore.get(begin).get(split);
//                    HashMap<Integer, Double> rightBinaryScores = unaryScore.get(split).get(end);
                    HashMap<Integer, Double> leftBinaryScores = score.get(begin).get(split);
                    HashMap<Integer, Double> rightBinaryScores = score.get(split).get(end);

                    for (Integer B: leftBinaryScores.keySet()) {
                        for (BinaryRule AtoBC: grammar.getBinaryRulesByLeftChild(B)) {
                            int A = AtoBC.getParent();
                            //DEBUG
                            System.out.println(" A->BC rule: " + AtoBC + "(" + labelIndexer.get(A)
                                    + " -> " + labelIndexer.get(AtoBC.getLeftChild()) + "-" +
                                    labelIndexer.get(AtoBC.getRightChild()) + "), p=" + AtoBC.getScore());
                            int C = AtoBC.getRightChild();

                            // check whether right child's score exists
                            if (! rightBinaryScores.containsKey(C)) {
                                continue;
                            }

                            double prob = leftBinaryScores.get(B) + rightBinaryScores.get(C) + AtoBC.getScore();
//                            HashMap<Integer, Double> binaryScoreMap = binaryScore.get(begin).get(end);
                            HashMap<Integer, Double> binaryScoreMap = score.get(begin).get(end);
                            if ( !(binaryScoreMap.containsKey(A)) || (prob > binaryScoreMap.get(A)) ) {
//                                DEBUG
//                                System.out.println(" A->BC rule: " + AtoBC + "(" + labelIndexer.get(A)
//                                        + " -> " + labelIndexer.get(AtoBC.getLeftChild()) + "-" +
//                                        labelIndexer.get(AtoBC.getRightChild()) + "), p=" + AtoBC.getScore());

                                // update score and back pointer
                                binaryScoreMap.put(A, prob);
                                System.out.println("    ----------<<<<<Updating BIscores entry [" + begin + "][" + end + "] " +
                                        "with k=" + A + " v=" + prob);
                                biBackPointer.get(begin).get(end).put(A, new Triple<>(split, B, C));
                                System.out.println("    ----------Updating BI Back pointer entry [" + begin + "][" + end + "] " +
                                        "with k=" + A + " v=" + split + "," + B + "," + C);
                            }
                        }
                    }

                }
                //handle unaries
                HashMap<Integer, Double> scoreMap = score.get(begin).get(end);
//                HashMap<Integer, Double> unaryScoreMap = unaryScore.get(begin).get(end);
                HashMap<Integer, Integer> uniBackPtrMap = uniBackPointer.get(begin).get(end);
                System.out.println("***\nUNARY CASE***");

//                handleUnaries(begin, end, unaryScoreMap, uniBackPtrMap);
//                unaryScore.get(begin).set(end, unaryScoreMap);
                handleUnaries(begin, end, scoreMap, uniBackPtrMap);
//                score.get(begin).set(end, scoreMap);
//                uniBackPointer.get(begin).set(end, uniBackPtrMap);
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
        System.out.println("--Handling Unaries...");
        Indexer<String> labelIndexer = grammar.getLabelIndexer();

        System.out.println("UnaryScoreMap is: " + unaryScoreMap);
        System.out.println("UnaryBackPtrMap is " + unaryBackPtrMap);

        boolean added = true;
        while (added) {
            added = false;
            // TODO: scan only valid tags B
            System.out.println("----------Processing this key set: " + unaryScoreMap.keySet());
            Set<Integer> keySet = new HashSet<>(unaryScoreMap.keySet());
            for (int B: keySet) {
                // get all A s.t. A -> B is a unary closure rule
                for (UnaryRule AtoB: unaryClosure.getClosedUnaryRulesByChild(B)) {
                    System.out.println("----------------Evaluating " + AtoB);
                    double p_A_to_B = AtoB.getScore();
                    double prob = p_A_to_B + unaryScoreMap.get(B);
                    int A = AtoB.getParent();
                    if ( (! unaryScoreMap.containsKey(A)) || (prob > unaryScoreMap.get(A)) ) {
                        // DEBUG
                        System.out.println(" A->B unary closure rule: " + AtoB +
                                "(" + labelIndexer.get(AtoB.getParent()) + "->" +
                                labelIndexer.get(AtoB.getChild()) + "), p=" + AtoB.getScore());
                        unaryScoreMap.put(A, prob);
                        System.out.println("    ----------(Handle)Updating UNIscores entry [" + begin + "][" + end + "] " +
                                "with k=" + A + " v=" + prob);
                        uniBackPointer.get(begin).get(end).put(A, B);
                        System.out.println("    ----------Updating UNI Backpointer entry [" + begin + "][" + end + "] " +
                                "with k=" + A + " v=" + B);
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
        System.out.println("\nBuiding the parsed tree...");
        Indexer<String> labelIndexer = grammar.getLabelIndexer();

        // base case
        if (end == start + 1) {
            System.out.println("Base case with start=" + start + " and end=" + end + " and parent=" + parent);
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
            System.out.println("Unary case with start=" + start + " and end=" + end + " and parent=" + parent);
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
            System.out.println("Binary case with start=" + start + " and end=" + end + " and parent=" + parent);
            Triple<Integer, Integer, Integer> triple = biBackPointer.get(start).get(end).get(parent);
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