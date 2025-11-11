package org.swam.sirio_mcp_server;

import org.oristool.petrinet.*;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Service;

@Service
public class SirioService {
    private PetriNet petriNet = null;

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

    @Tool(name = "show", description = "Show current petri net")
    public String showPetriNet() {
        return petriNet.toString();
    }

    @Tool(name = "add_precondition", description = "Add a precondition to a transition")
    public PetriNet addPrecondition(Place p, Transition t) {
        petriNet.addPrecondition(p, t);
        return petriNet;
    }

    @Tool(name = "add_postcondition", description = "Add a postcondition to a transition")
    public PetriNet addPostcondition(Transition t, Place p) {
        petriNet.addPostcondition(t, p);
        return petriNet;
    }


}
