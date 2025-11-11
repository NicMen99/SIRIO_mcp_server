package org.swam.sirio_mcp_server;

import org.oristool.models.stpn.trees.StochasticTransitionFeature;
import org.oristool.petrinet.*;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;

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

    @Tool(name = "add_transitions", description = "Add new transitions to the net")
    public PetriNet addTransitions(String ... transition_names) {
        for (String transitionName : transition_names) {
            petriNet.addTransition(transitionName);
        }
        return petriNet;
    }

    @Tool(name = "add_UNI", description = "Add a uniformely distributed transition")
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

    @Tool(name = "show_net", description = "Show current petri net")
    public String showPetriNet() {
        return petriNet.toString();
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

    @Tool(name = "show_marking", description = "Show current petri net")
    public String showMarking() {
        return marking.toString();
    }
}
