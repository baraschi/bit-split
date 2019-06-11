package EPFL;

import java.util.*;

public class ExtendedState extends State {

    private String value;

    private Set<State> set;

    private Set<State> acceptingStates;

    public ExtendedState(String state, boolean isAccepting, String value) {
        super(state, isAccepting);
        this.value = value;
        this.set = new HashSet<>();
        this.acceptingStates = new HashSet<>();
    }

    public void addSet(Set<State> other) {
        this.set.addAll(other);
    }

    @Override
    public void addState(State s) {
        this.set.add(s);
    }

    @Override
    public Set<State> getSet() {
        return this.set;
    }

    public String getValue() {
        return value;
    }

    public void setAccepting() {
        for (State s : set) {
            if (s.isAccepting()) {
                super.setAccepting(true);
                acceptingStates.add(s);
            }
        }
    }


    private ArrayList<String> sortedSet(Set<State> set) {
        ArrayList<String> a = new ArrayList<>();
        for (State s : set) {
            a.add(s.getState());
        }
        Collections.sort(a);
        return a;
    }

    @Override
    public String toString() {
        String states = sortedSet(acceptingStates).toString();
        return String.valueOf(getState()) + " {" + states.substring(1, states.length() - 1) + "}";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ExtendedState)) return false;
        ExtendedState that = (ExtendedState) o;
        //check that values are equal and set contents are equal
        return Objects.equals(value, that.value) && Objects.equals(getSet(), that.getSet());

    }

    @Override
    public int hashCode() {
        return Objects.hash(value, set);
    }

}
