
package EPFL;

import org.jgrapht.*;
import org.jgrapht.graph.*;

import org.jgrapht.io.*;
import org.jgrapht.traverse.DepthFirstIterator;
import org.jgrapht.traverse.GraphIterator;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;


public class BitSplit {
    private static String path = Constants.path;
    private enum Bits {B76, B54, B32, B10}

    public static void main(String[] args) {

        // Test graphs
        Graph<State, RelationshipEdge> D = createDGraph();
        saveGraph(D, path + "D_graph.dot");

        Graph<State, RelationshipEdge> origB3 = createOrigB3Graph();
        saveGraph(origB3, path + "OrigB3_graph.dot");

        // get binary encodings for alphabet characters
        Map<String, List<Integer>> binEncoding = getBinEncoding();

        // Graphs in paper https://ieeexplore.ieee.org/abstract/document/1431550
        Graph<ExtendedState, RelationshipEdge> B3 = getBitSplitGraph(D, binEncoding, 3, false);
        saveGraphExtended(B3, path + "B3_graph.dot");

        Graph<ExtendedState, RelationshipEdge> B4 = getBitSplitGraph(D, binEncoding, 4, false);
        saveGraphExtended(B4, path + "B4_graph.dot");

        Graph<ExtendedState, RelationshipEdge> B76 = get2BitSplitGraph(D, binEncoding, Bits.B76, false);
        saveGraphExtended(B76, path + "B76_graph.dot");

        Graph<ExtendedState, RelationshipEdge> B54 = get2BitSplitGraph(D, binEncoding, Bits.B54, false);
        saveGraphExtended(B54, path + "B54_graph.dot");

        Graph<ExtendedState, RelationshipEdge> B32 = get2BitSplitGraph(D, binEncoding, Bits.B32, false);
        saveGraphExtended(B32, path + "B32_graph.dot");

        Graph<ExtendedState, RelationshipEdge> B10 = get2BitSplitGraph(D, binEncoding, Bits.B10, false);
        saveGraphExtended(B10, path + "B10_graph.dot");

        // Graphs in paper https://ieeexplore.ieee.org/document/1639434
        Graph<State, RelationshipEdge> C = createCGraph();
        saveGraph(C, path + "C_graph.dot");

        Graph<ExtendedState, RelationshipEdge> C54 = get2BitSplitGraph(C, binEncoding, Bits.B54, true);
        saveGraphExtended(C54, path + "C54_graph.dot");

    }


    /*
     * Method which takes original graph g and applies bit split algorithm to return a new graph for the bit given as
     * a parameter.
     * As per: https://ieeexplore.ieee.org/abstract/document/1431550
     * Note that you can choose whether 0 or 7 is the LSB by setting the boolean zeroLSB
     */
    private static Graph<ExtendedState, RelationshipEdge> getBitSplitGraph(Graph<State, RelationshipEdge> g,
                                                                           Map<String, List<Integer>> binEncoding,
                                                                           int bitNumber, boolean zeroLSB) {
        //create new graph
        Graph<ExtendedState, RelationshipEdge> bitSplit = new DirectedPseudograph<>(RelationshipEdge.class);
        //iterate over states in g graph
        GraphIterator<State, RelationshipEdge> iterator = new DepthFirstIterator<>(g);
        //counter for state number
        int stateNumber = 0;
        //first node of g graph
        State source = iterator.next();
        //create new extended state corresponding to first node of bit split graph
        ExtendedState state = new ExtendedState(String.valueOf(stateNumber), false, "0");
        //add first node of g to set of nodes of first node of bit split graph
        state.addState(source);
        //add state to bit split
        bitSplit.addVertex(state);
        //increase state number
        stateNumber++;

        //set of nodes of bit split graph to visit
        Queue<ExtendedState> toVisit = new LinkedList<>();
        //add initial state to queue
        toVisit.add(state);

        //loop through until there are no more states to visit
        while (!toVisit.isEmpty()) {
            //set of nodes with transition state 0
            Set<State> zeros = new HashSet<>();
            //set of nodes with transition state 1
            Set<State> ones = new HashSet<>();

            //get and remove next element from toVisit queue
            ExtendedState currentState = toVisit.poll();

            assert currentState != null;

            //loop through set of states of state
            for (State s : currentState.getSet()) {
                //get outgoing edged of current state
                Set<RelationshipEdge> outgoing = g.outgoingEdgesOf(s);
                //loop through outgoing edges of state
                for (RelationshipEdge e : outgoing) {
                    //get edge label
                    String label = e.getLabel();
                    //find corresponding integer value (0 or 1) for bit given on input
                    Integer labelValue;
                    if(zeroLSB) {
                        labelValue = Math.abs(binEncoding.get(label).get(bitNumber)-7);
                    }else{
                        labelValue = binEncoding.get(label).get(bitNumber);
                    }
                    if (labelValue == 0) {
                        //add reachable state to 0 set
                        zeros.add(e.getTarget());
                    } else {
                        //add reachable state to 1 set
                        ones.add(e.getTarget());
                    }
                }
            }

            if (!zeros.isEmpty()) {
                //check if there is an existing state for zeros state set
                zeros.add(source);
                ExtendedState zero = stateInGraph(bitSplit, zeros, "0");
                //if not, create and add new extended state
                if (Objects.isNull(zero)) {
                    zero = new ExtendedState(String.valueOf(stateNumber), false, "0");
                    zero.addSet(zeros);
                    stateNumber++;
                    bitSplit.addVertex(zero);
                    toVisit.add(zero);
                }
                //add edge between current state and extended state
                if (noExistingEdge(bitSplit, currentState, "0")) {
                    bitSplit.addEdge(currentState, zero, new RelationshipEdge(String.valueOf(0)));
                }
            }

            if (!ones.isEmpty()) {
                //check if there is an existing states for ones state set
                ones.add(source);
                ExtendedState one = stateInGraph(bitSplit, ones, "1");
                //if not, create and add new extended state
                if (Objects.isNull(one)) {
                    one = new ExtendedState(String.valueOf(stateNumber), false, "1");
                    one.addSet(ones);
                    stateNumber++;
                    bitSplit.addVertex(one);
                    toVisit.add(one);
                }
                //add edge between current state and extended state
                if (noExistingEdge(bitSplit, currentState, "1")) {
                    bitSplit.addEdge(currentState, one, new RelationshipEdge(String.valueOf(1)));
                }
            }
        }
        for (ExtendedState e : bitSplit.vertexSet()) {
            e.setAccepting();
        }

        return bitSplit;

    }


    /*
     * Method which takes original graph g and applies bit split algorithm to return a new graph for the bits given as
     * a parameter.
     * As per: https://ieeexplore.ieee.org/document/1639434
     * Note that in this implementation, you can choose whether the LSB is 0 or 7 by setting the zerLSB boolean
     */
    private static Graph<ExtendedState, RelationshipEdge> get2BitSplitGraph(Graph<State, RelationshipEdge> g,
                                                                            Map<String, List<Integer>> binEncoding,
                                                                            Bits bitNumbers, boolean zeroLSB) {
        //create new graph
        Graph<ExtendedState, RelationshipEdge> bitSplit = new DirectedPseudograph<>(RelationshipEdge.class);
        //iterate over states in g graph
        GraphIterator<State, RelationshipEdge> iterator = new DepthFirstIterator<>(g);
        //counter for state number
        int stateNumber = 0;
        //first node of g graph
        State source = iterator.next();
        //create new extended state corresponding to first node of bit split graph
        ExtendedState state = new ExtendedState(String.valueOf(stateNumber), false, "0");
        //add first node of g to set of nodes of first node of bit split graph
        state.addState(source);
        //add state to bit split
        bitSplit.addVertex(state);

        //increase state number
        stateNumber++;

        //set of nodes of bit split graph to visit
        Queue<ExtendedState> toVisit = new LinkedList<>();
        //add initial state to queue
        toVisit.add(state);

        //loop through until there are no more states to visit
        while (!toVisit.isEmpty()) {
            //set of nodes with transition state 00
            Set<State> doubleZeros = new HashSet<>();
            //set of nodes with transition state 01
            Set<State> zeroOnes = new HashSet<>();
            //set of nodes with transition state 10
            Set<State> oneZeros = new HashSet<>();
            //set of nodes with transition state 11
            Set<State> doubleOnes = new HashSet<>();


            //get and remove next element from toVisit queue
            ExtendedState currentState = toVisit.poll();
            //Set<State> sorted = new TreeSet(currentState.getSet());

            //loop through set of states of state
            assert currentState != null;
            for (State s : currentState.getSet()) {
                //get outgoing edged of current state
                Set<RelationshipEdge> outgoing = g.outgoingEdgesOf(s);

                //loop through outgoing edges of state
                for (RelationshipEdge e : outgoing) {
                    //get edge label
                    String label = e.getLabel();
                    //find corresponding integer value (0 or 1) for bit given on input

                    String labelValue;
                    if (zeroLSB) {
                        labelValue = getBits0LSB(binEncoding.get(label), bitNumbers);
                    } else {
                        labelValue = getBits7LSB(binEncoding.get(label), bitNumbers);
                    }

                    switch (labelValue) {
                        case "00":
                            doubleZeros.add(e.getTarget());
                            break;
                        case "01":
                            zeroOnes.add(e.getTarget());
                            break;
                        case "10":
                            oneZeros.add(e.getTarget());
                            break;
                        case "11":
                            doubleOnes.add(e.getTarget());
                            break;
                    }
                }
            }

            //TODO: Refactor this
            //-----------------------------------------------------------------------
            //check if there is an existing state for doubleZeros state set
            if (!doubleZeros.isEmpty()) {
                doubleZeros.add(source);
                ExtendedState doubleZero = stateInGraph(bitSplit, doubleZeros, "00");
                //if not, create and add new extended state
                if (Objects.isNull(doubleZero)) {
                    doubleZero = new ExtendedState(String.valueOf(stateNumber), false, "00");
                    doubleZero.addSet(doubleZeros);
                    stateNumber++;
                    bitSplit.addVertex(doubleZero);
                    toVisit.add(doubleZero);
                }
                //add edge between current state and extended state
                if (noExistingEdge(bitSplit, currentState, "00")) {
                    bitSplit.addEdge(currentState, doubleZero, new RelationshipEdge("00"));
                }
            }

            if (!zeroOnes.isEmpty()) {
                //check if there is an existing state for zeroOnes state set
                zeroOnes.add(source);
                ExtendedState zeroOne = stateInGraph(bitSplit, zeroOnes, "01");
                //if not, create and add new extended state
                if (Objects.isNull(zeroOne)) {
                    zeroOne = new ExtendedState(String.valueOf(stateNumber), false, "01");
                    zeroOne.addSet(zeroOnes);
                    stateNumber++;
                    bitSplit.addVertex(zeroOne);
                    toVisit.add(zeroOne);
                }
                //add edge between current state and extended state
                if (noExistingEdge(bitSplit, currentState, "01")) {
                    bitSplit.addEdge(currentState, zeroOne, new RelationshipEdge("01"));
                }
            }

            if (!oneZeros.isEmpty()) {
                //check if there is an existing state for oneZeros state set
                oneZeros.add(source);
                ExtendedState oneZero = stateInGraph(bitSplit, oneZeros, "10");
                //if not, create and add new extended state
                if (Objects.isNull(oneZero)) {
                    oneZero = new ExtendedState(String.valueOf(stateNumber), false, "10");
                    oneZero.addSet(oneZeros);
                    stateNumber++;
                    bitSplit.addVertex(oneZero);
                    toVisit.add(oneZero);
                }
                //add edge between current state and extended state
                if (noExistingEdge(bitSplit, currentState, "10")) {
                    bitSplit.addEdge(currentState, oneZero, new RelationshipEdge("10"));
                }

            }

            if (!doubleOnes.isEmpty()) {
                //check if there is an existing states for doubleOnes state set
                doubleOnes.add(source);
                ExtendedState doubleOne = stateInGraph(bitSplit, doubleOnes, "11");
                //if not, create and add new extended state
                if (Objects.isNull(doubleOne)) {
                    doubleOne = new ExtendedState(String.valueOf(stateNumber), false, "11");
                    doubleOne.addSet(doubleOnes);
                    stateNumber++;
                    bitSplit.addVertex(doubleOne);
                    toVisit.add(doubleOne);
                }

                //add edge between current state and extended state
                if (noExistingEdge(bitSplit, currentState, "11")) {
                    bitSplit.addEdge(currentState, doubleOne, new RelationshipEdge("11"));
                }
            }
            //-----------------------------------------------------------------------

        }

        for (ExtendedState e : bitSplit.vertexSet()) {
            e.setAccepting();
        }

        return bitSplit;

    }

    private static String getBits0LSB(List<Integer> l, Bits b) {
        switch (b) {
            case B10:
                return l.get(6).toString() + l.get(7).toString();
            case B32:
                return l.get(4).toString() + l.get(5).toString();
            case B54:
                return l.get(2).toString() + l.get(3).toString();
            case B76:
                return l.get(0).toString() + l.get(1).toString();
        }
        return "";
    }

    private static String getBits7LSB(List<Integer> l, Bits b) {
        switch (b) {
            case B10:
                return l.get(0).toString() + l.get(1).toString();
            case B32:
                return l.get(2).toString() + l.get(3).toString();
            case B54:
                return l.get(4).toString() + l.get(5).toString();
            case B76:
                return l.get(6).toString() + l.get(7).toString();
        }
        return "";
    }


    /*
     * Helper method to see if given state does not have an edge with given value in graph g
     */
    private static boolean noExistingEdge(Graph<ExtendedState, RelationshipEdge> g, ExtendedState state, String value) {
        Set<RelationshipEdge> outgoing = g.outgoingEdgesOf(state);
        for (RelationshipEdge e : outgoing) {
            if (e.getLabel().equals(value)) {
                return false;
            }
        }
        return true;
    }

    /*
     * Helper method to see if there exists a state in graph g with same state set for given value
     */
    private static ExtendedState stateInGraph(Graph<ExtendedState, RelationshipEdge> graph, Set<State> states, String value) {
        GraphIterator<ExtendedState, RelationshipEdge> iterator = new DepthFirstIterator<>(graph);
        while (iterator.hasNext()) {
            ExtendedState source = iterator.next();
            if ((source.getSet().equals(states)) && value.equals(source.getValue())) {
                return source;
            }
        }
        return null;
    }


    /*
     * Helper method to get binary encoding
     */
    private static Map<String, List<Integer>> getBinEncoding() {

        Map<String, List<Integer>> binEncoding = new HashMap<>();
        binEncoding.put("=", Arrays.asList(0, 0, 1, 1, 1, 1, 0, 1));
        binEncoding.put("a", Arrays.asList(0, 1, 1, 0, 0, 0, 0, 1));
        binEncoding.put("c", Arrays.asList(0, 1, 1, 0, 0, 0, 1, 1));
        binEncoding.put("d", Arrays.asList(0, 1, 1, 0, 0, 1, 0, 0));
        binEncoding.put("e", Arrays.asList(0, 1, 1, 0, 0, 1, 0, 1));
        binEncoding.put("h", Arrays.asList(0, 1, 1, 0, 1, 0, 0, 0));
        binEncoding.put("i", Arrays.asList(0, 1, 1, 0, 1, 0, 0, 1));
        binEncoding.put("m", Arrays.asList(0, 1, 1, 0, 1, 1, 0, 1));
        binEncoding.put("n", Arrays.asList(0, 1, 1, 0, 1, 1, 1, 0));
        binEncoding.put("r", Arrays.asList(0, 1, 1, 1, 0, 0, 1, 0));
        binEncoding.put("s", Arrays.asList(0, 1, 1, 1, 0, 0, 1, 1));
        binEncoding.put("t", Arrays.asList(0, 1, 1, 1, 0, 1, 0, 0));
        binEncoding.put("x", Arrays.asList(0, 1, 1, 1, 1, 0, 0, 0));
        return binEncoding;
    }

    /*
     * Helper method to save State graph to file
     */
    private static void saveGraph(Graph<State, RelationshipEdge> graph, String path) {
        File file = new File(path);
        toDot(file, graph);
    }

    /*
     * Helper method to save ExtendedState graph to file
     */
    private static void saveGraphExtended(Graph<ExtendedState, RelationshipEdge> graph, String path) {
        File file = new File(path);
        toDotExtended(file, graph);
    }

    /*
     * Helper method to convert State graph to dot
     */
    private static void toDot(File file, Graph<State, RelationshipEdge> g) {
        FileWriter fw;


        // use helper classes to define how vertices should be rendered,
        // adhering to the DOT language restrictions
        ComponentNameProvider<State> vertexIdProvider = s -> String.valueOf(s.getState());

        ComponentAttributeProvider<State> vertexComponentAttributeProvider = component -> {
            Map<String, Attribute> m = new HashMap<>();
            m.put("label", DefaultAttribute.createAttribute(component.toString()));
            if (component.isAccepting()) {
                m.put("shape", DefaultAttribute.createAttribute("doublecircle"));
            } else {
                m.put("shape", DefaultAttribute.createAttribute("circle"));
            }
            return m;
        };


        ComponentNameProvider<State> vertexLabelProvider = s -> {

            String set = String.format("{%s}", String.join(", ", s.getSet().stream().map(State::getState).collect(Collectors.toList())));
            return s.getState() + (s.getSet().isEmpty() ? "" : set);

        };

        ComponentNameProvider<RelationshipEdge> edgeLabelProvider = RelationshipEdge::getLabel;

        try {
            DOTExporter<State, RelationshipEdge> exporter = new DOTExporter<>(vertexIdProvider, vertexLabelProvider,
                    edgeLabelProvider, vertexComponentAttributeProvider, null);
            fw = new FileWriter(file);
            exporter.exportGraph(g, fw);
            fw.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    /*
     * Helper method to convert ExtendedState graph to dot
     */
    private static void toDotExtended(File file, Graph<ExtendedState, RelationshipEdge> g) {
        FileWriter fw;


        // use helper classes to define how vertices should be rendered,
        // adhering to the DOT language restrictions
        ComponentNameProvider<ExtendedState> vertexIdProvider = s -> String.valueOf(s.getState());

        ComponentAttributeProvider<ExtendedState> vertexComponentAttributeProvider = component -> {
            Map<String, Attribute> m = new HashMap<>();
            m.put("label", DefaultAttribute.createAttribute(component.toString()));
            if (component.isAccepting()) {
                m.put("shape", DefaultAttribute.createAttribute("doublecircle"));
            } else {
                m.put("shape", DefaultAttribute.createAttribute("circle"));
            }

            return m;
        };


        ComponentNameProvider<ExtendedState> vertexLabelProvider = ExtendedState::toString;

        ComponentNameProvider<RelationshipEdge> edgeLabelProvider = RelationshipEdge::getLabel;


        try {
            DOTExporter<ExtendedState, RelationshipEdge> exporter =
                    new DOTExporter<>(vertexIdProvider, vertexLabelProvider, edgeLabelProvider,
                            vertexComponentAttributeProvider, null);
            fw = new FileWriter(file);
            exporter.exportGraph(g, fw);
            fw.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    /*
     * Helper method which returns B3 graph as defined in paper https://ieeexplore.ieee.org/abstract/document/1431550
     */
    private static Graph<State, RelationshipEdge> createOrigB3Graph() {
        Graph<State, RelationshipEdge> origB3 = new DirectedPseudograph<>(RelationshipEdge.class);
        State b0 = new State("0");
        origB3.addVertex(b0);

        State b1 = new State("1");
        origB3.addVertex(b1);

        State b2 = new State("2");
        origB3.addVertex(b2);

        State b3 = new State("3", true);
        origB3.addVertex(b3);

        State b4 = new State("4");
        origB3.addVertex(b4);

        State b5 = new State("5", true);
        origB3.addVertex(b5);

        State b6 = new State("6", true);
        origB3.addVertex(b6);

        State b7 = new State("7", true);
        origB3.addVertex(b7);

        // -- State 0
        origB3.addEdge(b0, b1, new RelationshipEdge("0"));
        origB3.addEdge(b0, b2, new RelationshipEdge("1"));


        // -- State 1
        origB3.addEdge(b1, b3, new RelationshipEdge("0"));
        origB3.addEdge(b1, b2, new RelationshipEdge("1"));

        // -- State 2
        origB3.addEdge(b2, b4, new RelationshipEdge("0"));
        origB3.addEdge(b2, b2, new RelationshipEdge("1"));

        // -- State 3
        origB3.addEdge(b3, b3, new RelationshipEdge("0"));
        origB3.addEdge(b3, b5, new RelationshipEdge("1"));

        // -- State 4
        origB3.addEdge(b4, b6, new RelationshipEdge("0"));
        origB3.addEdge(b4, b2, new RelationshipEdge("1"));

        // -- State 5
        origB3.addEdge(b5, b4, new RelationshipEdge("0"));
        origB3.addEdge(b5, b7, new RelationshipEdge("1"));

        // -- State 6
        origB3.addEdge(b6, b3, new RelationshipEdge("0"));
        origB3.addEdge(b6, b5, new RelationshipEdge("1"));

        // -- State 7
        origB3.addEdge(b7, b4, new RelationshipEdge("0"));
        origB3.addEdge(b7, b2, new RelationshipEdge("1"));

        return origB3;
    }

    /*
     * Helper method which returns D graph as defined in paper https://ieeexplore.ieee.org/abstract/document/1431550
     */
    private static Graph<State, RelationshipEdge> createDGraph() {
        Graph<State, RelationshipEdge> D = new DirectedPseudograph<>(RelationshipEdge.class);


        State s0 = new State("0");
        D.addVertex(s0);

        State s1 = new State("1");
        D.addVertex(s1);

        State s2 = new State("2", true);
        D.addVertex(s2);

        State s3 = new State("3");
        D.addVertex(s3);

        State s4 = new State("4");
        D.addVertex(s4);

        State s5 = new State("5", true);
        D.addVertex(s5);

        State s6 = new State("6");
        D.addVertex(s6);

        State s7 = new State("7", true);
        D.addVertex(s7);

        State s8 = new State("8", true);
        D.addVertex(s8);

        State s9 = new State("9", true);
        D.addVertex(s9);


        // -- State 0
        D.addEdge(s0, s1, new RelationshipEdge("h"));
        D.addEdge(s0, s3, new RelationshipEdge("s"));


        // -- State 1
        D.addEdge(s1, s1, new RelationshipEdge("h"));
        D.addEdge(s1, s3, new RelationshipEdge("s"));
        D.addEdge(s1, s2, new RelationshipEdge("e"));
        D.addEdge(s1, s6, new RelationshipEdge("i"));

        // -- State 2
        D.addEdge(s2, s3, new RelationshipEdge("s"));
        D.addEdge(s2, s1, new RelationshipEdge("h"));
        D.addEdge(s2, s8, new RelationshipEdge("r"));

        // -- State 3
        D.addEdge(s3, s3, new RelationshipEdge("s"));
        D.addEdge(s3, s4, new RelationshipEdge("h"));

        // -- State 4
        D.addEdge(s4, s1, new RelationshipEdge("h"));
        D.addEdge(s4, s3, new RelationshipEdge("s"));
        D.addEdge(s4, s5, new RelationshipEdge("e"));
        D.addEdge(s4, s6, new RelationshipEdge("i"));

        // -- State 5
        D.addEdge(s5, s3, new RelationshipEdge("s"));
        D.addEdge(s5, s1, new RelationshipEdge("h"));
        D.addEdge(s5, s8, new RelationshipEdge("r"));

        // -- State 6
        D.addEdge(s6, s1, new RelationshipEdge("h"));
        D.addEdge(s6, s7, new RelationshipEdge("s"));

        // -- State 7
        D.addEdge(s7, s3, new RelationshipEdge("s"));
        D.addEdge(s7, s4, new RelationshipEdge("h"));


        // -- State 8
        D.addEdge(s8, s1, new RelationshipEdge("h"));
        D.addEdge(s8, s9, new RelationshipEdge("s"));


        // -- State 9
        D.addEdge(s9, s3, new RelationshipEdge("s"));
        D.addEdge(s9, s4, new RelationshipEdge("h"));
        return D;
    }

    /*
     * Helper method which returns D graph as defined in paper https://ieeexplore.ieee.org/abstract/document/1431550
     */
    private static Graph<State, RelationshipEdge> createCGraph() {
        Graph<State, RelationshipEdge> D = new DirectedPseudograph<>(RelationshipEdge.class);


        State s0 = new State("0");
        D.addVertex(s0);

        State s1 = new State("1");
        D.addVertex(s1);

        State s2 = new State("2");
        D.addVertex(s2);

        State s3 = new State("3", true);
        D.addVertex(s3);

        State s4 = new State("4");
        D.addVertex(s4);

        State s5 = new State("5");
        D.addVertex(s5);

        State s6 = new State("6", true);
        D.addVertex(s6);

        State s7 = new State("7");
        D.addVertex(s7);

        State s8 = new State("8");
        D.addVertex(s8);

        State s9 = new State("9", true);
        D.addVertex(s9);

        State s10 = new State("10");
        D.addVertex(s10);

        State s11 = new State("11");
        D.addVertex(s11);

        State s12 = new State("12", true);
        D.addVertex(s12);


        // -- State 0
        D.addEdge(s0, s1, new RelationshipEdge("c"));
        D.addEdge(s0, s4, new RelationshipEdge("e"));
        D.addEdge(s0, s10, new RelationshipEdge("n"));


        // -- State 1
        D.addEdge(s1, s2, new RelationshipEdge("a"));
        D.addEdge(s1, s7, new RelationshipEdge("m"));


        // -- State 2
        D.addEdge(s2, s3, new RelationshipEdge("t"));


        // -- State 3 - no outgoing edges


        // -- State 4
        D.addEdge(s4, s5, new RelationshipEdge("t"));


        // -- State 5
        D.addEdge(s5, s6, new RelationshipEdge("="));


        // -- State 6 - no outgoing edges


        // -- State 7
        D.addEdge(s7, s8, new RelationshipEdge("d"));


        // -- State 8
        D.addEdge(s8, s9, new RelationshipEdge("d"));


        // -- State 9  - no outgoing edges


        // -- State 10
        D.addEdge(s10, s11, new RelationshipEdge("e"));


        // -- State 11
        D.addEdge(s11, s12, new RelationshipEdge("t"));


        // -- State 12
        D.addEdge(s12, s6, new RelationshipEdge("="));

        return D;
    }

}
