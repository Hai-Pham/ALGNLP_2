package edu.berkeley.nlp.assignments.parsing.student;

import java.util.List;

import edu.berkeley.nlp.assignments.parsing.BaselineParser;
import edu.berkeley.nlp.assignments.parsing.Parser;
import edu.berkeley.nlp.assignments.parsing.ParserFactory;
import edu.berkeley.nlp.ling.Tree;


public class GenerativeParserFactory implements ParserFactory {
	
	public Parser getParser(List<Tree<String>> trainTrees) {

		 return new BaselineParser(trainTrees);
	}

}
