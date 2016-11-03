package edu.berkeley.nlp.assignments.parsing.student;

import edu.berkeley.nlp.assignments.parsing.*;
import edu.berkeley.nlp.assignments.parsing.student.util.Lexicon;
import edu.berkeley.nlp.assignments.parsing.student.util.Grammar;
import edu.berkeley.nlp.assignments.parsing.student.util.TreeMarkovAnnotation;
import edu.berkeley.nlp.assignments.parsing.student.util.Triple;
import edu.berkeley.nlp.ling.Tree;
import edu.berkeley.nlp.util.Indexer;

import java.util.*;

public class CKYNaiveParser implements Parser
{
    public static class CKYNaiveParserFactory implements ParserFactory {

        public Parser getParser(List<Tree<String>> trainTrees) {
            return new edu.berkeley.nlp.assignments.parsing.student.CKYNaiveParser(trainTrees);
        }
    }

    // =========================FIELDS=========================
    // List of score and back pointer tables
    // Key of all internal LinkedHashMaps are the index of Non-Terminal Symbols
    // (managed by grammar's labelIndexer)
    private static ArrayList<ArrayList<LinkedHashMap<Integer, Double>>> unaryScore, binaryScore, score;
    private static ArrayList<ArrayList<LinkedHashMap<Integer, Integer>>> uniBackPointer;
    private static ArrayList<ArrayList<LinkedHashMap<Integer, Triple>>> biBackPointer;
    private static UnaryClosure unaryClosure;

    Lexicon lexicon;
    edu.berkeley.nlp.assignments.parsing.student.util.Grammar grammar;
    int numNonTerminals;

    // Markovization constant
    // TODO: make these arguments from a command line for facilitate tests
    private final int vOrder = 2;
    private final int hOrder = 2;
    // =========================END OF FIELDS=========================


    /**
     * ACCCESSING DRIVER FOR THE TEST
     * Given a sentence, yield a best parse in terms of Tree data structure
     * The most important method - will be called by a Test Case
     * @param sentence
     * @return Tree<String> of best parse
     */
    public Tree<String> getBestParse(List<String> sentence) {
        cky(sentence);
        // uncapble of parsing correctly
        if (unaryScore.get(0).get(sentence.size()).get(0) == null)
            return new Tree<>("ROOT", Collections.singletonList(new Tree<>("JUNK")));

        Tree<String> annotatedBestParse = createCKYParsedTree(sentence, 0, false, 0, sentence.size());
        return TreeMarkovAnnotation.unAnnotateTree(annotatedBestParse);
    }

    // =========================CONSTRUCTOR=========================
    /**
     * Constructor
     * @param trainTrees
     * Also: init grammar, lexicon and unary closure which can be used alot later without re-init
     */
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
     * Helper for constructor, do the markovization of trees before training (and so testing)
     * @param trees
     * @return
     */
    private List<Tree<String>> annotateTrees(List<Tree<String>> trees) {
        List<Tree<String>> annotatedTrees = new ArrayList<>();
        for (Tree<String> tree : trees) {
            annotatedTrees.add(TreeMarkovAnnotation.annotateTree(tree, vOrder, hOrder));
        }
        return annotatedTrees;
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
    // =========================END OF CONSTRUCTOR=========================

    // =========================DEBUGGING METHODS =========================
    public void debugLexiconToConsole(){
//        CounterMap<String, String> lexiconMap = lexicon.getLexicon();
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
            System.out.println(parent + "(" + labelIndexer.indexOf(parent) + ")" +
                    " ==> " + child + "(" + labelIndexer.indexOf(child) + ")");
        }
    }
    public void printBinaryRules() {
        List<BinaryRule> binaryRules = grammar.getBinaryRules();
        System.out.println("Binary rules are: \n");
        System.out.println(binaryRules);
    }
    public void debugScoreTablesToConsole(ArrayList<ArrayList<LinkedHashMap<Integer, Double>>> score){
        System.out.println("SCORE Table");
        int c=0;
        for (List<LinkedHashMap<Integer, Double>>  listScoreMap: score) {
            System.out.println("*** Level " + c++);
            int cc = 0;
            for (LinkedHashMap<Integer, Double> scoreMap : listScoreMap)
                System.out.println("=== Sublevel =" + cc++  + "==="  + scoreMap);
        }
        System.out.println();
    }
    public void debugBackPointerTablesToConsole() {
        System.out.println("Unary Back Pointer Table:");
        int c=0;
        for (List<LinkedHashMap<Integer, Integer>> listUnaryBackPtr: uniBackPointer) {
            System.out.println("*** Level " + c++);
            int cc = 0;
            for (LinkedHashMap<Integer, Integer> unaryBackPtr : listUnaryBackPtr)
                System.out.println("=== Sublevel =" + cc++  + "===" + unaryBackPtr);
        }
        System.out.println();
        System.out.println("Binary Back Pointer Table:");
        c=0;
        for (List<LinkedHashMap<Integer, Triple>> listBinaryBackPtr: biBackPointer) {
            System.out.println("*** Level " + c++);
            int cc = 0;
            for (LinkedHashMap<Integer, Triple> binaryBackPtr : listBinaryBackPtr)
                System.out.println("=== Sublevel =" + cc++  + "===" + binaryBackPtr);
        }
        System.out.println();
    }
    void printKeysetOfLinkedHashMap(LinkedHashMap<Integer, Double> map) {
        for (Integer k: map.keySet()) {
            System.out.println("\n\n\nk= " + k + " v= " + map.get(k));
        }
    }
    // =========================END OF DEBUGGING METHODS =========================

    // =========================GETTERS METHODS =========================
    public Lexicon getLexicon() {
        return lexicon;
    }
    public UnaryClosure getUnaryClosure() {
        return unaryClosure;
    }
    public Grammar getGrammar() {
        return grammar;
    }
    public static ArrayList<ArrayList<LinkedHashMap<Integer, Double>>> getUnaryScore() {
        return unaryScore;
    }
    public static ArrayList<ArrayList<LinkedHashMap<Integer, Double>>> getBinaryScore() {
        return binaryScore;
    }
    public static ArrayList<ArrayList<LinkedHashMap<Integer, Integer>>> getUniBackPointer() {
        return uniBackPointer;
    }
    public static ArrayList<ArrayList<LinkedHashMap<Integer, Triple>>> getBiBackPointer() {
        return biBackPointer;
    }
    // =========================END OF GETTERS METHODS =========================


    /**
     * Init scores and back pointers tables for CKY
     * This will be called by CKY, the content of the 3 static tables will be changed w.r.t
     * the sentence being dealt with
     * TODO: change back to private after debugging
     * @param sentence: input of CKY
     */
    public void initScoreAndBackPointerTables(List<String> sentence) {
        unaryScore = new ArrayList<>();
        binaryScore = new ArrayList<>();
        uniBackPointer = new ArrayList<>();
        biBackPointer = new ArrayList<>();
        int n = sentence.size();

        for (int i=0; i<n+1; i++) {
            ArrayList<LinkedHashMap<Integer, Double>> unaryScoreList = new ArrayList<>();
            ArrayList<LinkedHashMap<Integer, Double>> binaryScoreList = new ArrayList<>();
            ArrayList<LinkedHashMap<Integer, Integer>> uniList = new ArrayList<>();
            ArrayList<LinkedHashMap<Integer, Triple>> biList = new ArrayList<>();

            for (int j = 0; j < n + 1; j++) {
                LinkedHashMap<Integer, Double> unaryScoreMap = new LinkedHashMap<>();
                LinkedHashMap<Integer, Double> binaryScoreMap = new LinkedHashMap<>();
                LinkedHashMap<Integer, Integer> uniBackPtrMap = new LinkedHashMap<>();
                LinkedHashMap<Integer, Triple> biBackPtrMap = new LinkedHashMap<>();
                // build n ArrayList of LinkedHashMap\
                unaryScoreList.add(unaryScoreMap);
                binaryScoreList.add(binaryScoreMap);
                uniList.add(uniBackPtrMap);
                biList.add(biBackPtrMap);
            }
            // build n ArrrayList of ArrayList
            unaryScore.add(unaryScoreList);
            binaryScore.add(binaryScoreList);
            uniBackPointer.add(uniList);
            biBackPointer.add(biList);
        }
    }


    /**
     * TODO: refactor this mess
     * CKY implememtation, given grammar and lexicon
     * This algorithm interleaves binary and unary cases, facilitating the building parse tree later
     * @param sentence as a list of string
     * @return null, it will update the private tables of scores and back pointers
     */
    public void cky(List<String> sentence) {

        // Init essential data structures
        initScoreAndBackPointerTables(sentence);
        Indexer<String> labelIndexer = grammar.getLabelIndexer();
        int n = sentence.size();
        for (int i = 0; i < n; i++) {
//             process all pre-terminals
            for (int tag = 0; tag < numNonTerminals; tag++) {
                double tagScore = lexicon.scoreTagging(sentence.get(i), labelIndexer.get(tag));
//                if (Double.isFinite(tagScore)) {
                if ( (! Double.isNaN(tagScore)) & (tagScore != Double.NEGATIVE_INFINITY) ) {
                    unaryScore.get(i).get(i + 1).put(tag, tagScore);
                }
            }
        }
        // Done Tag => Word now checking whether A=> Tag
        // handle unaries of [i][i+1]
        for (int i = 0; i < n; i++) {
            handleUnaries(i);
        }
        // MAIN PROCESSING
        // Alternating between binaries and unaries
        for (int span = 2; span <= n; span++) {
            for (int begin = 0; begin <= (n - span); begin++) {
                int end = begin + span;

                LinkedHashMap<Integer, Double> binaryScoreMap = binaryScore.get(begin).get(end);
                for (int split= begin + 1; split <= end - 1; split++) {

                    LinkedHashMap<Integer, Double> leftUnaryScores = unaryScore.get(begin).get(split);
                    LinkedHashMap<Integer, Double> rightUnaryScores = unaryScore.get(split).get(end);

                    for (Integer B: leftUnaryScores.keySet()) {
                        for (BinaryRule AtoBC: grammar.getBinaryRulesByLeftChild(B)) {
                            int A = AtoBC.getParent();
                            int C = AtoBC.getRightChild();
                            // check whether right child's score exists
                            if (! rightUnaryScores.containsKey(C)) {
                                continue;
                            }
                            double prob = leftUnaryScores.get(B) + rightUnaryScores.get(C) + AtoBC.getScore();
                            if ( !(binaryScoreMap.containsKey(A)) || (prob > binaryScoreMap.get(A)) ) {
                                // update score and back pointer
                                binaryScoreMap.put(A, prob);
                                biBackPointer.get(begin).get(end).put(A, new Triple<>(split, B, C));
                            }
                        }
                    }
                }
                // Done A -> BC now check whether D -> A
                handleUnariesForBinary(begin, end, binaryScoreMap);
            }
        }
    }

    /**
     * Handle Unary Case in CKY
     * TODO: switch back to private after debugging
     * @param i index
     */
    public void handleUnaries(int i) {
        LinkedHashMap<Integer, Double> unaryScoreMap = unaryScore.get(i).get(i+1);
        LinkedHashMap<Integer, Integer> unaryBackPtrMap = uniBackPointer.get(i).get(i+1);

            Set<Integer> keySet = new LinkedHashSet<>(unaryScoreMap.keySet()); // deep copy to avoid exception
            for (int B: keySet) {
                // get all A s.t. A -> B is a unary closure rule
                for (UnaryRule AtoB: unaryClosure.getClosedUnaryRulesByChild(B)) {
                    double p_A_to_B = AtoB.getScore();
                    double prob = p_A_to_B + unaryScoreMap.get(B);
                    int A = AtoB.getParent();
                    if ( (! unaryScoreMap.containsKey(A)) || (prob > unaryScoreMap.get(A)) ) {
                        unaryScoreMap.put(A, prob);
                        unaryBackPtrMap.put(A, B);
                    }
                }
            }
    }

    /**
     * This Unary handle is different from the first one as it will compare with the score in binaryScore
     * table before updating to unary tables
     * @param begin
     * @param end
     * @param binaryScoreMap at location [begin][end]
     */
    public void handleUnariesForBinary(int begin, int end, LinkedHashMap<Integer, Double> binaryScoreMap) {
        LinkedHashMap<Integer, Double> unaryScoreMap = unaryScore.get(begin).get(end);
//        LinkedHashMap<Integer, Double> binaryScoreMap = binaryScore.get(begin).get(end);
        LinkedHashMap<Integer, Integer> unaryBackPtrMap = uniBackPointer.get(begin).get(end);

        Set<Integer> keySet = new LinkedHashSet<>(binaryScoreMap.keySet()); // deep copy to avoid exception
        for (int B : keySet) {
            // get all A s.t. A -> B is a unary closure rule
            for (UnaryRule AtoB : unaryClosure.getClosedUnaryRulesByChild(B)) {
                double p_A_to_B = AtoB.getScore();
                double prob = p_A_to_B + binaryScoreMap.get(B);
                int A = AtoB.getParent();
                if ((!unaryScoreMap.containsKey(A)) || (prob > unaryScoreMap.get(A))) {
                    unaryScoreMap.put(A, prob);
                    unaryBackPtrMap.put(A, B);
                }
            }
        }
    }

    /**
     * Create a parsed tree after CKY Parsing recursively
     * TODO: refactor this mess
     */
    public Tree<String> createCKYParsedTree(List<String> sentence, int parent, boolean isBinaryTurn,
                                    int start, int end) {
        Indexer<String> labelIndexer = grammar.getLabelIndexer();
        // base case
        if (end == start + 1) {
            // terminals (words)
            if (uniBackPointer.get(start).get(end).get(parent) == null)
                // parent -> word
                return new Tree<>(labelIndexer.get(parent), Arrays.asList(new Tree<>(sentence.get(start))));
            else {
                // parent -> tag -> word
                int tag = uniBackPointer.get(start).get(end).get(parent);
                Tree<String> tagToWordTree = new Tree<>(labelIndexer.get(tag),
                        Arrays.asList(new Tree<>(sentence.get(start))));

                // expansion of unary rule
                UnaryRule unaryRule = new UnaryRule(parent, tag);
                List<Integer> path = unaryClosure.getPath(unaryRule);
                if (path.size() > 2) {
                    System.out.println("Path bigger than 2 ");
                    for (int i = 1; i < path.size() -1 ; i++) {
                        Tree<String> tmpTree = new Tree<>(labelIndexer.get(path.get(i)), Arrays.asList(tagToWordTree));
                        tagToWordTree = tmpTree;
                    }
                }
                // look for possible parents
                return new Tree<>(labelIndexer.get(parent), Arrays.asList(tagToWordTree));
            }
        }

        Tree<String> masterTree = new Tree<>(labelIndexer.get(parent));
        // unary
        if (! isBinaryTurn) {
            Tree<String> unaryTree;
            if (!uniBackPointer.get(start).get(end).containsKey(parent)) {
                masterTree = createCKYParsedTree(sentence, parent, true, start, end);
            }
            else {
                // get the back point of parent
                int child = uniBackPointer.get(start).get(end).get(parent);
                // handle reflexive rule
                if (child == parent) {
                    masterTree = createCKYParsedTree(sentence, parent, true, start, end);
                } else {
                    // expansion of unary rule
                    UnaryRule unaryRule = new UnaryRule(parent, child);
                    List<Integer> path = unaryClosure.getPath(unaryRule);
                    unaryTree = createCKYParsedTree(sentence, child, true, start, end);
                    if (path.size() > 2) {
                        System.out.println("Path bigger than 2 ");
                        for (int i = 1; i < path.size() - 1; i++) {
                            Tree<String> tmpTree = new Tree<>(labelIndexer.get(path.get(i)), Arrays.asList(unaryTree));
                            unaryTree = tmpTree;
                        }
                    }
                    masterTree.setChildren(Arrays.asList(unaryTree));
                }

            }
        }
        else {
        // binary
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