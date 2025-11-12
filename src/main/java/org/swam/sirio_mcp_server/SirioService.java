package org.swam.sirio_mcp_server;

import org.oristool.models.stpn.trees.StochasticTransitionFeature;
import org.oristool.petrinet.*;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Service
public class SirioService {
    private PetriNet petriNet = null;
    private Marking marking = null;

    @Tool(name="create", description = "Create an empty petri net")
    public PetriNet createPetriNet() {
        petriNet =  new PetriNet();
        return petriNet;
    }

    @Tool(name = "add_places", description = "Add new places to the net")
    public PetriNet addPlaces(String ... node_names) {
        for (String nodeName : node_names) {
            petriNet.addPlace(nodeName);
        }
        return petriNet;
    }

    @Tool(name = "remove_places", description = "Remove existent places from the net")
    public PetriNet removePlaces(String ... node_names) {
        List<Place> places_to_be_removed = new ArrayList<Place>();
        for (String nodeName : node_names) {
            Place p = petriNet.getPlace(nodeName);
            places_to_be_removed.add(p);
        }
        for (Place place : places_to_be_removed) {
            petriNet.removePlace(place);
        }
        return petriNet;
    }

    @Tool(name = "add_transitions", description = "Add new transitions to the net")
    public PetriNet addTransitions(String ... transition_names) {
        for (String transitionName : transition_names) {
            petriNet.addTransition(transitionName);
        }
        return petriNet;
    }

    @Tool(name = "remove_transitions", description = "Remove existent transitions from the net")
    public PetriNet removeTransitions(String ... transition_names) {
        List<Transition> transitions_to_be_removed = new ArrayList<Transition>();
        for (String transitionName : transition_names) {
            Transition t = petriNet.getTransition(transitionName);
            transitions_to_be_removed.add(t);
        }
        for (Transition transition : transitions_to_be_removed) {
            Collection<Precondition> affected_preconds = petriNet.getPreconditions(transition);
            for (Precondition precond : affected_preconds) {
                petriNet.removePrecondition(precond);
            }
            Collection<Postcondition> affected_postconds = petriNet.getPostconditions(transition);
            for (Postcondition postcond : affected_postconds) {
                petriNet.removePostcondition(postcond);
            }
            petriNet.removeTransition(transition);
        }
        return petriNet;
    }

    @Tool(name = "add_UNI", description = "Add a new uniformely distributed transition")
    public void addUNI(
            @ToolParam(description = "name of transition") String transition_name,
            @ToolParam(description = "earliest firing time") String etf,
            @ToolParam(description = "latest firing time") String ltf
    ) {
        Transition t = petriNet.getTransitions().stream()
                .filter(trans -> trans.getName().equals(transition_name))
                .findFirst()
                .orElse(null);
        if (t == null) {
            t = petriNet.addTransition(transition_name);
        }

        t.addFeature(StochasticTransitionFeature.newUniformInstance(etf, ltf));
    }

    @Tool(name = "add_DET", description = "Add a new transition with a deterministic timer")
    public void addDET(
            @ToolParam(description = "name of transition") String transition_name,
            @ToolParam(description = "timer value") String value
    ) {
        Transition t = petriNet.getTransitions().stream()
                .filter(trans -> trans.getName().equals(transition_name))
                .findFirst()
                .orElse(null);
        if (t == null) {
            t = petriNet.addTransition(transition_name);
        }

        t.addFeature(StochasticTransitionFeature.newDeterministicInstance(value));
    }

    @Tool(name = "show_net", description = "Show the current status of the Petri Net")
    public String showPetriNet() {
        return petriNet.toString() + "\n" + marking.toString();
    }

    @Tool(name = "add_precondition", description = "Add a precondition to a transition")
    public PetriNet addPrecondition(
        @ToolParam(description = "Name of the place") String place_name,
        @ToolParam(description = "Name of the transition") String transition_name
    ){
        // Trova il place per nome 
        Place p = petriNet.getPlaces().stream()
            .filter(place -> place.getName().equals(place_name))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Place not found" + place_name));
        

        // Trova la transition per nome
        Transition t = petriNet.getTransitions().stream()
            .filter(trans -> trans.getName().equals(transition_name))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Transition not found" + transition_name));

        petriNet.addPrecondition(p, t);
        return petriNet;
    }

    @Tool(name = "remove_preconditions", description = "Remove a precondition to a transition")
    public PetriNet removePrecondition(
            @ToolParam(description = "Name of the place") String place_name,
            @ToolParam(description = "Name of the transition") String transition_name
    ){
        // Trova il place per nome
        Place p = petriNet.getPlaces().stream()
                .filter(place -> place.getName().equals(place_name))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Place not found" + place_name));


        // Trova la transition per nome
        Transition t = petriNet.getTransitions().stream()
                .filter(trans -> trans.getName().equals(transition_name))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Transition not found" + transition_name));

        Precondition pc = petriNet.getPrecondition(p, t);
        petriNet.removePrecondition(pc);
        return petriNet;
    }

    @Tool(name = "add_postcondition", description = "Add a postcondition to a transition")
    public PetriNet addPostcondition(
        @ToolParam(description="Name of the transition") String transition_name,
        @ToolParam(description="Name of the place") String place_name
    ) {
        // Trova la transition per nome
        Transition t = petriNet.getTransitions().stream()
            .filter(trans -> trans.getName().equals(transition_name))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Transition not found" + transition_name));

        // Trova il place per nome 
        Place p = petriNet.getPlaces().stream()
            .filter(place -> place.getName().equals(place_name))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Place not found" + place_name));
        petriNet.addPostcondition(t, p);
        return petriNet;
    }

    @Tool(name = "remove_postconditions", description = "Remove a postcondition to a transition")
    public PetriNet removePostcondition(
            @ToolParam(description = "Name of the place") String place_name,
            @ToolParam(description = "Name of the transition") String transition_name
    ){
        // Trova la transition per nome
        Transition t = petriNet.getTransitions().stream()
                .filter(trans -> trans.getName().equals(transition_name))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Transition not found" + transition_name));

        // Trova il place per nome
        Place p = petriNet.getPlaces().stream()
                .filter(place -> place.getName().equals(place_name))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Place not found" + place_name));

        Postcondition pc = petriNet.getPostcondition(t, p);
        petriNet.removePostcondition(pc);
        return petriNet;
    }

    @Tool(name = "create_marking", description = "Creates an empty marking for the current Petri Net")
    public Marking createMarking() {
        marking = new Marking();
        return marking;
    }

    @Tool(name = "add_tokens", description = "Add a specific number of tokens to a place")
    public Marking addToken(
            @ToolParam(description = "Name of the place") String name,
            @ToolParam(description = "Number of tokens to be added") int num
    ) {
        Place p = petriNet.getPlaces().stream()
                .filter(place -> place.getName().equals(name))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Place not found" + name));
        marking.addTokens(p, num);
        return marking;
    }

    @Tool(name = "add_inhibitor_arc", description = "Add an inhibitor arc between a place and a transition")
    public PetriNet addInhibitorArc(
            @ToolParam(description = "Name of the source place") String source_name,
            @ToolParam(description = "Name of the target transition") String transition_name
    ) {
        Place source = petriNet.getPlaces().stream()
                .filter(place -> place.getName().equals(source_name))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Place not found" + source_name));

        Transition target = petriNet.getTransitions().stream()
                .filter(trans -> trans.getName().equals(transition_name))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Transition not found" + transition_name));

        petriNet.addInhibitorArc(source, target);
        return petriNet;
    }
}
