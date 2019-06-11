package EPFL;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

public class State  implements Comparable<State>{

    private boolean isAccepting;
    private String state;
    private Set<State> set;

    public State(String state) {
        this.state = state;
        this.isAccepting = false;
        set = new HashSet<>();
    }

    public State(String state, boolean isAccepting) {
        this.state = state;
        this.isAccepting = isAccepting;
        set = new HashSet<>();
    }


    public String getState() {
        return state;
    }

    public boolean isAccepting() {
        return isAccepting;
    }

    public Set<State> getSet() {
        return set;
    }

    public void setAccepting(boolean accepting) {
        isAccepting = accepting;
    }

    public void addState(State s) {
        set.add(s);
    }

    @Override
    public String toString() {
        return "State{" +
                "state=" + state +
                ", isAccepting=" + isAccepting +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof State)) return false;
        State state1 = (State) o;
        return isAccepting() == state1.isAccepting() &&
                Objects.equals(getState(), state1.getState()) &&
                Objects.equals(getSet(), state1.getSet());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getState(), isAccepting(), getSet());
    }

    @Override
    public int compareTo(State o) {
        Integer a = Integer.valueOf(this.getState());
        Integer b = Integer.valueOf(o.getState());
        if(a > b){
            return 1;
        }
        if(b > a){
            return -1;
        }
        return 0;
    }
}
