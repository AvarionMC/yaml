package org.avarion.yaml.testClasses;

import org.avarion.yaml.YamlFileInterface;
import org.avarion.yaml.YamlKey;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Test class for records containing collections of other records.
 */
public class TeamConfigClass extends YamlFileInterface {

    @YamlKey("team")
    public Team team;

    @YamlKey("teams")
    public Map<String, Team> teams = new LinkedHashMap<>();

    public TeamConfigClass() {
        // Create team members
        List<Person> alphaMembers = new ArrayList<>();
        alphaMembers.add(new Person("Alice", 28, new Address("123 Alpha St", "Alpha City", 11111)));
        alphaMembers.add(new Person("Bob", 35, new Address("456 Alpha Ave", "Alpha City", 11112)));

        // Create single team
        team = new Team("Alpha Team", alphaMembers, new Address("1 HQ Blvd", "Alpha City", 10000));

        // Create map of teams
        List<Person> betaMembers = new ArrayList<>();
        betaMembers.add(new Person("Charlie", 30, new Address("789 Beta St", "Beta City", 22221)));
        betaMembers.add(new Person("Diana", 25, new Address("321 Beta Ave", "Beta City", 22222)));
        betaMembers.add(new Person("Eve", 40, new Address("654 Beta Rd", "Beta City", 22223)));

        teams.put("alpha", team);
        teams.put("beta", new Team("Beta Team", betaMembers, new Address("2 HQ Blvd", "Beta City", 20000)));
    }
}
