package edu.berkeley.nlp.assignments.parsing.student.test;


import edu.berkeley.nlp.assignments.parsing.Parser;
import edu.berkeley.nlp.assignments.parsing.UnaryRule;
import edu.berkeley.nlp.assignments.parsing.student.*;
import edu.berkeley.nlp.assignments.parsing.student.util.Grammar;
import edu.berkeley.nlp.assignments.parsing.student.util.Lexicon;
import edu.berkeley.nlp.io.PennTreebankReader;
import edu.berkeley.nlp.ling.Tree;
import edu.berkeley.nlp.ling.Trees;
import edu.berkeley.nlp.parser.EnglishPennTreebankParseEvaluator;
import edu.berkeley.nlp.util.CommandLineUtils;

import java.util.*;

/**
 * Harness for PCFG Parser project.
 *
 * @author Dan Klein
 */
public class CKYNaiveParserConciseTester
{

    private static Grammar grammar;
    private static Lexicon lexicon;

    public static void main(String[] args) {
        // Parse command line flags and arguments
        Map<String, String> argMap = CommandLineUtils.simpleCommandLineParser(args);

        // Set up default parameters and settings
        String basePath = ".";
        int maxTrainLength = 40;
        int maxTestLength = 40;

        // train and test indexes
//        int start = 219;
//        int end = 219;
        int start = 200;
        int end = 205;

        System.out.println("Doing something with HandleUnaries...");
        List<String> smallSentence = new ArrayList<>(Arrays.asList("Ms.", "Haag", "plays", "Elianti", "."));
//        List<String> smallSentence = new ArrayList<>(Arrays.asList("Tuesday", ",", "October", "31", ",", "1989"));
        System.out.println("Small sentence as a list of string " + smallSentence);


        // Update defaults using command line specifications
        if (argMap.containsKey("-path")) {
            basePath = argMap.get("-path");
            System.out.println("Using base path: " + basePath);
        }

        if (argMap.containsKey("-maxTrainLength")) {
            maxTrainLength = Integer.parseInt(argMap.get("-maxTrainLength"));
        }
        System.out.println("Maximum length for training sentences: " + maxTrainLength);
        if (argMap.containsKey("-maxTestLength")) {
            maxTestLength = Integer.parseInt(argMap.get("-maxTestLength"));
        }
        System.out.println("Maximum length for test sentences: " + maxTestLength);

        System.out.print("Loading training trees  ... ");
        List<Tree<String>> trainTrees = readTrees(basePath, start, end, maxTrainLength);
        System.out.println("done. (" + trainTrees.size() + " trees)");

        System.out.print("Loading test trees  ... ");
        List<Tree<String>> testTrees = null;
        testTrees = readTrees(basePath, start, end, maxTestLength);
        System.out.println("done. (" + testTrees.size() + " trees)");

        CKYNaiveParser parser = new CKYNaiveParser(trainTrees);
        grammar = parser.getGrammar();
        lexicon = parser.getLexicon();
//        parser.debugLexiconToConsole();
//        parser.getBestParse(smallSentence);
//        parser.cky(smallSentence);
//        parser.debugScoreTablesToConsole(parser.getScore());
//        parser.debugScoreTablesToConsole(parser.getUnaryScore());
//        parser.debugScoreTablesToConsole(parser.getBinaryScore());
//        parser.debugBackPointerTablesToConsole();
//        Tree<String> annotatedBestParse = parser.createCKYParsedTree(smallSentence, 0, false, 0, smallSentence.size());


//        PCFGParser parser = new PCFGParser();
//        parser.train(trainTrees);
//        Tree<String> t = parser.getBestParse(smallSentence);
//        System.out.println(Trees.PennTreeRenderer.render(t));
//
//        parser.debugScore();
//        parser.debugBack();


        System.out.println("\n\n\n\n\nEVALUATING PARSER NOW ...");
        testParser(parser, testTrees, true);

//        System.out.println(parser.getUnaryClosure().getPath(new UnaryRule(2, 3)));

    }

    private static void testParser(Parser parser, List<Tree<String>> testTrees, boolean verbose) {
        long nanos = System.nanoTime();
        EnglishPennTreebankParseEvaluator.LabeledConstituentEval<String> eval = new EnglishPennTreebankParseEvaluator.LabeledConstituentEval<String>(
                Collections.singleton("ROOT"), new HashSet<String>(Arrays.asList(new String[] { "''", "``", ".", ":", "," })));
        for (Tree<String> testTree : testTrees) {
            List<String> testSentence = testTree.getYield();
            Tree<String> guessedTree = parser.getBestParse(testSentence);
            if (verbose) {
                System.out.println("Guess:\n" + Trees.PennTreeRenderer.render(guessedTree));
                System.out.println("Gold:\n" + Trees.PennTreeRenderer.render(testTree));
            }
            eval.evaluate(guessedTree, testTree);
        }
        eval.display(true);
        System.out.println("Decoding took " + (System.nanoTime() - nanos)/1000000 + " millis");
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


}
