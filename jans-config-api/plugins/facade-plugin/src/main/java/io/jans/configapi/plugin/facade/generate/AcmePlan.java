/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2026, Janssen Project
 */

package io.jans.configapi.plugin.facade.generate;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public final class AcmePlan {

    public record User(String givenName, String inum, List<String> roles, String team) {
        public String workloadId() {
            return givenName + "-" + inum;
        }
    }

    public record Item(String action, JSONObject resource, JSONObject context, boolean anomaly) {
        public String capability() {
            return action + "|" + resource.getJSONObject("cedar_entity_mapping").getString("entity_type");
        }
    }

    public record Call(User user, List<Item> items) {
    }

    public record Execution(String id, String phase, List<Call> calls) {
        public int expectedRecords() {
            return calls.stream().mapToInt(c -> c.items().size()).sum();
        }
    }

    private AcmePlan() {
    }

    public static List<Execution> build(String runId, AcmeInputs in) {
        Random rng = new Random(in.seed());
        List<String> teams = new ArrayList<>(in.teamActions().keySet());
        Collections.sort(teams);
        List<Execution> plan = new ArrayList<>();
        for (int i = 0; i < in.trainExecutions(); i++) {
            plan.add(execution(rng, in, teams, "train", id(runId, plan.size())));
        }
        for (int i = 0; i < in.testExecutions(); i++) {
            plan.add(execution(rng, in, teams, "test", id(runId, plan.size())));
        }
        List<User> actors = in.users().stream().filter(u -> u.team().equals(in.anomalyTeam())).toList();
        for (int i = 0; i < in.anomalies(); i++) {
            User actor = actors.get(i % actors.size());
            Item item = item(rng, in, in.anomalyAction(), actor, false, true);
            plan.add(new Execution(id(runId, plan.size()), "test", List.of(new Call(actor, List.of(item)))));
        }
        return plan;
    }

    private static Execution execution(Random rng, AcmeInputs in, List<String> teams, String phase, String id) {
        String team = teams.get(rng.nextInt(teams.size()));
        List<User> pool = new ArrayList<>(in.users().stream().filter(u -> u.team().equals(team)).toList());
        Collections.shuffle(pool, rng);
        int size = Math.min(pool.size(), 2 + rng.nextInt(3));
        List<User> members = new ArrayList<>(pool.subList(0, size));
        if (rng.nextDouble() < in.crossTeamRate()) {
            List<User> others = in.users().stream().filter(u -> !u.team().equals(team)).toList();
            if (!others.isEmpty()) {
                members.add(others.get(rng.nextInt(others.size())));
            }
        }
        List<Call> calls = new ArrayList<>();
        for (User member : members) {
            calls.add(call(rng, in, member));
        }
        return new Execution(id, phase, calls);
    }

    private static Call call(Random rng, AcmeInputs in, User user) {
        String action = in.teamActions().get(user.team());
        int count = 1 + rng.nextInt(3);
        List<Item> items = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            items.add(item(rng, in, action, user, rng.nextDouble() < in.denyRate(), false));
        }
        return new Call(user, items);
    }

    private static Item item(Random rng, AcmeInputs in, String name, User user, boolean deny, boolean anomaly) {
        AcmeInputs.ActionTemplate t = in.actions().get(name);
        String environment = in.environments().get(rng.nextInt(in.environments().size()));
        Map<String, Object> vars = new HashMap<>();
        vars.put("given_name", user.givenName());
        vars.put("inum", user.inum());
        vars.put("customer", "customer-" + (1 + rng.nextInt(in.customers())));
        vars.put("workload", in.workloads().get(rng.nextInt(in.workloads().size())));
        vars.put("environment", environment);
        vars.put("is_prod", environment.equals("prod"));
        JSONObject resource = (JSONObject) fill(t.resource(), vars);
        JSONObject context = (JSONObject) fill(t.context(), vars);
        if (deny) {
            for (String key : t.denyContext().keySet()) {
                context.put(key, t.denyContext().get(key));
            }
        }
        return new Item(t.action(), resource, context, anomaly);
    }

    private static Object fill(Object node, Map<String, Object> vars) {
        if (node instanceof JSONObject object) {
            JSONObject copy = new JSONObject();
            for (String key : object.keySet()) {
                copy.put(key, fill(object.get(key), vars));
            }
            return copy;
        }
        if (node instanceof JSONArray array) {
            JSONArray copy = new JSONArray();
            for (int i = 0; i < array.length(); i++) {
                copy.put(fill(array.get(i), vars));
            }
            return copy;
        }
        if (node instanceof String text) {
            if (text.startsWith("${") && text.endsWith("}") && text.indexOf("${", 1) < 0) {
                return value(vars, text.substring(2, text.length() - 1));
            }
            String out = text;
            for (String key : vars.keySet()) {
                out = out.replace("${" + key + "}", String.valueOf(vars.get(key)));
            }
            if (out.contains("${")) {
                throw new IllegalStateException("unknown variable in input: " + out);
            }
            return out;
        }
        return node;
    }

    private static Object value(Map<String, Object> vars, String key) {
        if (!vars.containsKey(key)) {
            throw new IllegalStateException("unknown variable in input: ${" + key + "}");
        }
        return vars.get(key);
    }

    private static String id(String runId, int index) {
        return "exec-" + runId + "-" + String.format("%04d", index);
    }
}
