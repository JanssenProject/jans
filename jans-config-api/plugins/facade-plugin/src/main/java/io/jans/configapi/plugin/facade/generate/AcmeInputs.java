/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2026, Janssen Project
 */

package io.jans.configapi.plugin.facade.generate;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public record AcmeInputs(
        String executionAuthority,
        String clientId,
        long seed,
        int trainExecutions,
        int testExecutions,
        int anomalies,
        long pauseMillis,
        double denyRate,
        double crossTeamRate,
        int customers,
        List<String> workloads,
        List<String> environments,
        Map<String, ActionTemplate> actions,
        Map<String, String> teamActions,
        String anomalyTeam,
        String anomalyAction,
        List<AcmePlan.User> users) {

    public record ActionTemplate(String action, JSONObject resource, JSONObject context, JSONObject denyContext) {
    }

    public static final String FILE_NAME = "authorization.json";

    public static AcmeInputs load(Path file) throws IOException {
        if (!Files.isRegularFile(file)) {
            throw new IllegalStateException("authorization input file not found: " + file);
        }
        try {
            return parse(new JSONObject(Files.readString(file)));
        } catch (JSONException e) {
            throw new IllegalStateException("invalid authorization input " + file + ": " + e.getMessage(), e);
        }
    }

    static AcmeInputs parse(JSONObject json) {
        JSONObject traffic = json.getJSONObject("traffic");
        List<AcmePlan.User> users = new ArrayList<>();
        JSONArray array = json.getJSONArray("users");
        for (int i = 0; i < array.length(); i++) {
            JSONObject u = array.getJSONObject(i);
            List<String> roles = new ArrayList<>();
            JSONArray r = u.optJSONArray("roles");
            for (int j = 0; r != null && j < r.length(); j++) {
                roles.add(r.getString(j));
            }
            users.add(new AcmePlan.User(u.getString("given_name"), u.getString("inum"), roles,
                    u.getString("team")));
        }
        List<String> workloads = new ArrayList<>();
        JSONArray w = json.getJSONArray("workloads");
        for (int i = 0; i < w.length(); i++) {
            workloads.add(w.getString(i));
        }
        List<String> environments = new ArrayList<>();
        JSONArray env = json.getJSONArray("environments");
        for (int i = 0; i < env.length(); i++) {
            environments.add(env.getString(i));
        }
        Map<String, ActionTemplate> actions = new LinkedHashMap<>();
        JSONObject at = json.getJSONObject("actions");
        for (String name : at.keySet()) {
            JSONObject a = at.getJSONObject(name);
            actions.put(name, new ActionTemplate(a.getString("action"), a.getJSONObject("resource"),
                    a.getJSONObject("context"), a.optJSONObject("deny_context", new JSONObject())));
        }
        Map<String, String> teamActions = new LinkedHashMap<>();
        JSONObject ta = json.getJSONObject("team_actions");
        for (String team : ta.keySet()) {
            teamActions.put(team, ta.getString(team));
        }
        JSONObject anomaly = json.getJSONObject("anomaly");
        AcmeInputs inputs = new AcmeInputs(
                json.getString("execution_authority"),
                json.getString("client_id"),
                traffic.getLong("seed"),
                traffic.getInt("train_executions"),
                traffic.getInt("test_executions"),
                traffic.getInt("anomalies"),
                traffic.optLong("pause_millis", 0L),
                traffic.getDouble("deny_rate"),
                traffic.getDouble("cross_team_rate"),
                json.getInt("customers"),
                List.copyOf(workloads),
                List.copyOf(environments),
                Map.copyOf(actions),
                Map.copyOf(teamActions),
                anomaly.getString("team"),
                anomaly.getString("action"),
                List.copyOf(users));
        inputs.validate();
        return inputs;
    }

    private void validate() {
        if (users.isEmpty() || workloads.isEmpty() || environments.isEmpty() || customers < 1) {
            throw new IllegalStateException("users, workloads, environments and customers must not be empty");
        }
        for (String team : teamActions.keySet()) {
            if (users.stream().filter(u -> u.team().equals(team)).count() < 2) {
                throw new IllegalStateException("team " + team + " needs at least 2 users");
            }
            checkAction(teamActions.get(team));
        }
        checkAction(anomalyAction);
        if (users.stream().noneMatch(u -> u.team().equals(anomalyTeam))) {
            throw new IllegalStateException("anomaly.team has no users: " + anomalyTeam);
        }
    }

    private void checkAction(String name) {
        if (!actions.containsKey(name)) {
            throw new IllegalStateException("action " + name + " is not defined under \"actions\"");
        }
    }
}
