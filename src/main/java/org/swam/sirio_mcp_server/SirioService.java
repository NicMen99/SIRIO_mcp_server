package org.swam.sirio_mcp_server;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.oristool.models.gspn.GSPNSteadyState;
import org.oristool.models.gspn.GSPNTransient;
import org.oristool.models.stpn.trees.StochasticTransitionFeature;
import org.oristool.models.pn.PetriTokensAdder;
import org.oristool.models.pn.PetriTokensRemover;
import org.oristool.petrinet.EnablingFunction;
import org.oristool.petrinet.InhibitorArc;
import org.oristool.petrinet.Marking;
import org.oristool.petrinet.PetriNet;
import org.oristool.petrinet.Place;
import org.oristool.petrinet.Postcondition;
import org.oristool.petrinet.Precondition;
import org.oristool.petrinet.Transition;
import org.oristool.util.Pair;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;
import org.swam.pn_utils.PetriNetUtils;

@Service
public class SirioService {

    // --------------------------
    // Attributes
    // --------------------------
    private PetriNet petriNet = null;
    private Marking marking = null;

    @Tool(name="create", description = "Create an empty petri net with an empty marking")
    public void createPetriNet() {
        petriNet =  new PetriNet();
        marking = new Marking();
    }

    // --------------------------
    // Places
    // --------------------------

    @Tool(name = "add_places", description = "Add new places to the net")
    public PetriNet addPlaces(List<String> node_names) {
        for (String nodeName : node_names) {
            petriNet.addPlace(nodeName);
        }
        return petriNet;
    }

    @Tool(name = "remove_places", description = "Remove existent places from the net")
    public PetriNet removePlaces(List<String> node_names) {
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

    // --------------------------
    // Transitions
    // --------------------------

    @Tool(name = "add_transitions", description = "Add new transitions to the net")
    public PetriNet addTransitions(List<String> transition_names) {
        for (String transitionName : transition_names) {
            petriNet.addTransition(transitionName);
        }
        return petriNet;
    }

    @Tool(name = "remove_transitions", description = "Remove existent transitions from the net")
    public PetriNet removeTransitions(List<String> transition_names) {
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
            Collection<InhibitorArc> affected_inhibitor_arcs = petriNet.getInhibitorArcs(transition);
            for (InhibitorArc inhibitor_arc : affected_inhibitor_arcs) {
                petriNet.removeInhibitorArc(inhibitor_arc);
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
        Transition t = PetriNetUtils.findOrCreateTransitionByName(petriNet, transition_name);

        t.addFeature(StochasticTransitionFeature.newUniformInstance(etf, ltf));
    }

    @Tool(name = "add_DET", description = "Add a new transition with a deterministic timer")
    public void addDET(
            @ToolParam(description = "name of transition") String transition_name,
            @ToolParam(description = "timer value") String value
    ) {
        Transition t = PetriNetUtils.findOrCreateTransitionByName(petriNet, transition_name);

        t.addFeature(StochasticTransitionFeature.newDeterministicInstance(value));
    }

    @Tool(name = "add_IMM", description = "Add a new immediate transition")
    public void addIMM(
            @ToolParam(description = "name of transition") String transition_name
    ) {
        Transition t = PetriNetUtils.findOrCreateTransitionByName(petriNet, transition_name);

        t.addFeature(StochasticTransitionFeature.newDeterministicInstance("0"));
    }

    @Tool(name = "add_EXP", description = "Add a new transition with a exponential timer and variable rate")
    public void addEXP(
            @ToolParam(description = "name of transition") String transition_name,
            @ToolParam(description = "rate value") BigDecimal rate
    ) {
        Transition t = PetriNetUtils.findOrCreateTransitionByName(petriNet, transition_name);

        t.addFeature(StochasticTransitionFeature.newExponentialInstance(rate));
    }

    // --------------------------
    // Petri Net manipulation
    // --------------------------

    @Tool(name = "show_net", description = "Show the current status of the Petri Net")
    public String showPetriNet() {
        return petriNet.toString() + "\n" + marking.toString();
    }

    @Tool(name = "reset_marking", description = "Reset the current marking of the system to empty")
    public void resetMarking() {
        marking = new Marking();
    }

    @Tool(name = "add_tokens", description = "Add a specific number of tokens to a place")
    public Marking addToken(
            @ToolParam(description = "Name of the place") String name,
            @ToolParam(description = "Number of tokens to be added") int num
    ) {
        Place p = PetriNetUtils.findPlaceByName(petriNet, name);

        marking.addTokens(p, num);
        return marking;
    }

    @Tool(name = "remove_tokens", description = "Remove a specific number of tokens from a place")
    public Marking removeToken(
            @ToolParam(description = "Name of the place") String name,
            @ToolParam(description = "Number of tokens to be removed") int num
    ) {
        Place p = PetriNetUtils.findPlaceByName(petriNet, name);

        marking.removeTokens(p, num);
        return marking;
    }

    @Tool(name = "add_enabling_function", description = "Add an enabling function to a transition")
    public void addEnablingFunction(
            @ToolParam(description = "Condition of the enabling function, boolean expression as String (ex. 'place_name == 1')") String condition,
            @ToolParam(description = "Transition to apply the enabling function to")  String transition_name
    ) {
        Transition target = PetriNetUtils.findTransitionByName(petriNet, transition_name);

        target.addFeature(new EnablingFunction(condition));
    }

    @Tool(name = "get_enabled_transitions", description = "Get a list of transitions that are currently enabled and can be fired")
    public List<String> getEnabledTransitions() {
        if (petriNet == null || marking == null){
            throw new IllegalStateException("Petri net and marking must be created first.");
        }
        return petriNet.getEnabledTransitions(marking).stream()
                    .map(Transition::getName)
                    .collect(java.util.stream.Collectors.toList());
    }

    @Tool(name = "fire_transition", description = "Fires a specific transition to advance the marking (Token Game). Updates the current marking of the system")
    public String fireTransition(
        @ToolParam(description = "Name of the transition to fire") String transition_name
    ) {
        if (petriNet == null || marking == null) {
            throw new IllegalStateException("Petri net and marking must be created first.");
        }
        Transition t = PetriNetUtils.findTransitionByName(petriNet, transition_name);

        if (!petriNet.isEnabled(t, marking)) {  // Controllo che la transizione sia attiva
            throw new IllegalArgumentException("Transition " + transition_name + " is not enabled in the current marking.");
        }
        
        PetriTokensAdder pta = new PetriTokensAdder();
        pta.update(marking, petriNet, t);

        PetriTokensRemover ptr = new PetriTokensRemover();
        ptr.update(marking, petriNet, t);

        return marking.toString();
    }

    // --------------------------
    // Preconditions, Postconditions and Inhibitor Arcs
    // --------------------------
    @Tool(name = "add_precondition", description = "Add a precondition to a transition")
    public PetriNet addPrecondition(
        @ToolParam(description = "Name of the place") String place_name,
        @ToolParam(description = "Name of the transition") String transition_name
    ){
        Place p = PetriNetUtils.findPlaceByName(petriNet, place_name);
        Transition t = PetriNetUtils.findTransitionByName(petriNet, transition_name);

        petriNet.addPrecondition(p, t);
        return petriNet;
    }

    @Tool(name = "remove_preconditions", description = "Remove a precondition to a transition")
    public PetriNet removePrecondition(
            @ToolParam(description = "Name of the place") String place_name,
            @ToolParam(description = "Name of the transition") String transition_name
    ){
        Place p = PetriNetUtils.findPlaceByName(petriNet, place_name);
        Transition t = PetriNetUtils.findTransitionByName(petriNet, transition_name);

        Precondition pc = petriNet.getPrecondition(p, t);
        petriNet.removePrecondition(pc);
        return petriNet;
    }

    @Tool(name = "add_postcondition", description = "Add a postcondition to a transition")
    public PetriNet addPostcondition(
        @ToolParam(description="Name of the transition") String transition_name,
        @ToolParam(description="Name of the place") String place_name
    ) {
        Transition t = PetriNetUtils.findTransitionByName(petriNet, transition_name);
        Place p = PetriNetUtils.findPlaceByName(petriNet, place_name);

        petriNet.addPostcondition(t, p);
        return petriNet;
    }

    @Tool(name = "remove_postconditions", description = "Remove a postcondition to a transition")
    public PetriNet removePostcondition(
            @ToolParam(description = "Name of the place") String place_name,
            @ToolParam(description = "Name of the transition") String transition_name
    ){
        Transition t = PetriNetUtils.findTransitionByName(petriNet, transition_name);
        Place p = PetriNetUtils.findPlaceByName(petriNet, place_name);

        Postcondition pc = petriNet.getPostcondition(t, p);
        petriNet.removePostcondition(pc);
        return petriNet;
    }

    @Tool(name = "add_inhibitor_arc", description = "Add an inhibitor arc between a place and a transition")
    public PetriNet addInhibitorArc(
            @ToolParam(description = "Name of the source place") String source_name,
            @ToolParam(description = "Name of the target transition") String transition_name
    ) {
        Place source = PetriNetUtils.findPlaceByName(petriNet, source_name);
        Transition target = PetriNetUtils.findTransitionByName(petriNet, transition_name);

        petriNet.addInhibitorArc(source, target);
        return petriNet;
    }

    @Tool(name = "remove_inhibitor_arc", description = "Remove an inhibitor arc between a place and a transition")
    public PetriNet removeInhibitorArc(
        @ToolParam(description = "Name of the place") String place_name,
        @ToolParam(description = "Name of the transition") String transition_name
    ) {
        Place p = PetriNetUtils.findPlaceByName(petriNet, place_name);
        Transition t = PetriNetUtils.findTransitionByName(petriNet, transition_name);
        
        InhibitorArc ia = petriNet.getInhibitorArc(p, t);
        petriNet.removeInhibitorArc(ia);
        return petriNet;
    }

    // --------------------------
    // Analyses
    // --------------------------

    @Tool(name = "execute_steady_state_analysis", description = "Executes a steady state analysis on a generalized stochastic petri net. This requires all the transitions to be immediate (with firing time deterministic and equal to 0) or exponential (with firing time distributed as an exponential random variable with rate lambda)")
    public Map<Marking, Double> executeSteadyStateAnalysis() {
        return GSPNSteadyState.builder().build().compute(petriNet, marking);
    }

    @Tool(name = "execute_transient_analysis", description = "Executes a transient analysis on a generalized stochastic petri net at specific time points. This requires all the transitions to be immediate (with firing time deterministic and equal to 0) or exponential (with firing time distributed as an exponential random variable with rate lambda) and either a list of time points or a time range (start, end, step). Returns the probability distribution over markings at each specified time point.")
    public Map<Double, Map<Marking, Double>> executeTransientAnalysis(
            @ToolParam(description = "A list of time points to compute probabilities for (e.g., [1.0, 5.0, 10.0]) or a time range (start, end, step) (e.g. given [0.0, 2.0, 0.2], the resulting time points will be [0.0, 0.2, 0.4, 0.6, 0.8, 1.0, 1.2, 1.4, 1.6, 1.8, 2.0])") List<Double> timePoints
    ) {
        if (petriNet == null || marking == null) {
            throw new IllegalStateException("Petri net and marking must be created before running analysis.");
        }

        // Il GSPNTransient.builder accetta un array di double[] --> Converto la lista di Double in un array di double 
        double[] timePointsArray = timePoints.stream()
                                             .mapToDouble(Double::doubleValue)
                                             .toArray();

        // Creo l'analizzatore ed eseguo i calcoli
        Pair<Map<Marking, Integer>, double[][]> result = GSPNTransient.builder()
            .timePoints(timePointsArray)            // Costruisce l'analizzatore
            .build().compute(petriNet, marking);    // Esegue il calcolo    
        
        // Estraggo i risultati
        Map<Marking, Integer> statePos = result.first();
        double[][] probs = result.second(); // [indice_tempo][indice_stato]

        Map<Double, Map<Marking, Double>> transientResults = new HashMap<>();

        // Ciclo principale sugli istanti di tempo (corrispondenti alle righe della matrice)
        for (int t_idx = 0; t_idx < timePointsArray.length; t_idx++) {
            double currentTime = timePointsArray[t_idx];
            Map<Marking, Double> probsAtThisTime = new HashMap<>();

            // Per ogni istante, estraiamo le probabilità di tutti gli stati
            for (Map.Entry<Marking, Integer> entry : statePos.entrySet()) {
                Marking m = entry.getKey();
                int state_idx = entry.getValue(); // Indice di colonna
                
                if (t_idx < probs.length && state_idx < probs[t_idx].length) {
                    double prob = probs[t_idx][state_idx];
                    probsAtThisTime.put(m, prob);   // Potrei mettere un controllo prob > 0.0 per pulire l'output?
                }
            }
            transientResults.put(currentTime, probsAtThisTime);
        }
        return transientResults;
    }
}
