package edu.berkeley.nlp.assignments.parsing.student.test;

import edu.berkeley.nlp.assignments.parsing.student.util.Triple;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Created by Gorilla on 10/19/2016.
 */
public class SandboxTester {

    public static void main(String[] args){

        // Test score table
        System.out.println("Testing score table");
        ArrayList<ArrayList<HashMap<String, Double>>> score = new ArrayList<>();

        HashMap<String, Double> map1 = new HashMap<>();
        map1.put("a", 0.1);
        map1.put("b", 0.2);

        HashMap<String, Double> map2 = new HashMap<>();
        map2.put("c", 0.3);
        map2.put("d", 0.4);
        map2.put("e", 0.5);

        ArrayList<HashMap<String, Double>> intermMap1 = new  ArrayList<>();
        intermMap1.add(map1);

        ArrayList<HashMap<String, Double>> intermMap2 = new ArrayList<>();
        intermMap2.add(map2);
        intermMap2.add(map1);

        score.add(intermMap1);
        score.add(intermMap2);

        System.out.println(score);
        System.out.println(score.get(0));
        System.out.println(score.get(1).get(0));
        System.out.println(score.get(1).get(1));
        System.out.println();

        // Test back pointer table
        System.out.println("Testing back pointer table");
        ArrayList<ArrayList<HashMap<String, Object>>> back = new ArrayList<>();

        HashMap<String, Object> map3 = new HashMap<>();
        HashMap<String, Object> map4 = new HashMap<>();

        map3.put("a", "a");
        map3.put("b", 0.123);
        map3.put("c", new Integer(5));
        map3.put("d", new Triple(1, "a", "b"));

        map4.put("c", new Integer(5));
        map4.put("d", new Triple(1, "a", "b"));

        ArrayList<HashMap<String, Object>> intermMap3 = new  ArrayList<>();
        intermMap3.add(map3);

        ArrayList<HashMap<String, Object>> intermMap4 = new ArrayList<>();
        intermMap4.add(map3);
        intermMap4.add(map4);

        back.add(intermMap3);
        back.add(intermMap4);

        System.out.println(back);
//        System.out.println(back.get(3)); // Exception
    }


}
