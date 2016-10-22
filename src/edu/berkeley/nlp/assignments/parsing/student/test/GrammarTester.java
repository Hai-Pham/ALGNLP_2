package edu.berkeley.nlp.assignments.parsing.student.test;


import edu.berkeley.nlp.assignments.parsing.*;
import edu.berkeley.nlp.assignments.parsing.student.CKYNaiveParser;
import edu.berkeley.nlp.assignments.parsing.student.CoarseToFineParserFactory;
import edu.berkeley.nlp.assignments.parsing.student.GenerativeParserFactory;
import edu.berkeley.nlp.assignments.parsing.student.util.Grammar;
import edu.berkeley.nlp.assignments.parsing.student.util.Lexicon;
import edu.berkeley.nlp.io.PennTreebankReader;
import edu.berkeley.nlp.ling.Tree;
import edu.berkeley.nlp.ling.Trees;
import edu.berkeley.nlp.parser.EnglishPennTreebankParseEvaluator;
import edu.berkeley.nlp.util.CollectionUtils;
import edu.berkeley.nlp.util.CommandLineUtils;
import edu.berkeley.nlp.util.Indexer;

import java.util.*;

/**
 * Harness for PCFG Parser project.
 *
 * @author Dan Klein
 */
public class GrammarTester
{
    private static Grammar grammar;
    private static Lexicon lexicon;

    public enum ParserType
    {
        BASELINE
                {
                    @Override
                    public ParserFactory getParserFactory() {
                        return new BaselineParser.BaselineParserFactory();
                    }
                },
        GENERATIVE
                {
                    @Override
                    public ParserFactory getParserFactory() {
                        return new GenerativeParserFactory();
                    }
                },
        COARSE_TO_FINE
                {
                    @Override
                    public ParserFactory getParserFactory() {
                        return new CoarseToFineParserFactory();
                    }
                };

        public abstract ParserFactory getParserFactory();
    }

    public static void main(String[] args) {
        // Parse command line flags and arguments
        Map<String, String> argMap = CommandLineUtils.simpleCommandLineParser(args);

        // Set up default parameters and settings
        String basePath = ".";
        boolean verbose = true;
        boolean sanity = false;
        String testMode = "validate";
        int maxTrainLength = 1000;
        int maxTestLength = 40;

        if (argMap.containsKey("-sanityCheck")) {
            sanity = true;
        }

        // Update defaults using command line specifications
        if (argMap.containsKey("-path")) {
            basePath = argMap.get("-path");
            System.out.println("Using base path: " + basePath);
        }
        if (argMap.containsKey("-test")) {
            testMode = "test";
            System.out.println("Testing on final test data.");
        } else {
            System.out.println("Testing on validation data.");
        }
        if (argMap.containsKey("-maxTrainLength")) {
            maxTrainLength = Integer.parseInt(argMap.get("-maxTrainLength"));
        }
        System.out.println("Maximum length for training sentences: " + maxTrainLength);
        if (argMap.containsKey("-maxTestLength")) {
            maxTestLength = Integer.parseInt(argMap.get("-maxTestLength"));
        }
        System.out.println("Maximum length for test sentences: " + maxTestLength);
        if (argMap.containsKey("-verbose")) {
            verbose = true;
        }
        if (argMap.containsKey("-quiet")) {
            verbose = false;
        }
        edu.berkeley.nlp.assignments.parsing.PCFGParserTester.ParserType parserType = edu.berkeley.nlp.assignments.parsing.PCFGParserTester.ParserType.BASELINE;

        if (argMap.containsKey("-parserType")) {
            parserType = edu.berkeley.nlp.assignments.parsing.PCFGParserTester.ParserType.valueOf(argMap.get("-parserType"));
        }

        int trainTreesEnd = 2199;
        if (sanity) {
            maxTrainLength = 3;
            maxTestLength = 3;
            trainTreesEnd = 299;
        }
        System.out.print("Loading training trees (sections 2-21) ... ");
        List<Tree<String>> trainTrees = readTrees(basePath, 200, trainTreesEnd, maxTrainLength);
        System.out.println("done. (" + trainTrees.size() + " trees)");
        List<Tree<String>> testTrees = null;
        if (testMode.equalsIgnoreCase("validate")) {
            System.out.print("Loading validation trees (section 22) ... ");
            testTrees = readTrees(basePath, 2200, 2299, maxTestLength);
        } else {
            System.out.print("Loading test trees (section 23) ... ");
            testTrees = readTrees(basePath, 2300, 2399, maxTestLength);
        }
        System.out.println("done. (" + testTrees.size() + " trees)");

        CKYNaiveParser parser = new CKYNaiveParser(trainTrees);
        grammar = parser.getGrammar();
        lexicon = parser.getLexicon();

//        System.out.println("Now doing something with grammar...");
//        parser.debugGrammarToConsole();

//        System.out.println("Now doing something with lexicon...");
//        parser.debugLexiconToConsole();

        testDebugScoreTablesToConsole(parser);

//        testParserLexiconQuery();

//        testUnaryClosureQuery();

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

    private static void testDebugScoreTablesToConsole(CKYNaiveParser parser) {
        System.out.println("Now doing something with score tables...");
        List<String> smallSentence = new ArrayList<>(Arrays.asList("This", "is"));
        System.out.println(smallSentence);
        parser.initScoreAndBackPointerTables(smallSentence);
//        parser.debugScoreTablesToConsole();
    }

    private static void testParserLexiconQuery() {
        System.out.println("Testing with some lexicon tagging probability ");
        System.out.println(lexicon.scoreTagging("Ollie", "NNP"));
        System.out.println(lexicon.scoreTagging("XXXXX", "JJ"));
        System.out.println(lexicon.scoreTagging("carrier", "NN"));
        System.out.println(lexicon.getLexicon().getCount("XXXXX", "NN"));
        System.out.println(lexicon.getLexicon().getCount("carrier", "NN"));

        for (String s: lexicon.getAllTags()) {
            if (lexicon.getLexicon().getCount("suggest", s) > 0){
                System.out.println("Found tag " + s);
            }
        }
    }

    private static void testUnaryClosureQuery(){
        UnaryClosure uniClosure = new UnaryClosure(
                grammar.getLabelIndexer(), grammar.getUnaryRules());

        Map<UnaryRule, List<Integer>> pathMap = uniClosure.getPathMap();
        System.out.println(pathMap);
    }


}
