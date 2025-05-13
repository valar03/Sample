String groupDn = "CN=" + groupNameStr + ",OU=Groups,DC=example,DC=com";

Filter memberOfFilter = new EqualsFilter("memberOf", groupDn);
Filter objectClassFilter = new EqualsFilter("objectClass", "user");

AndFilter ldapFilter = new AndFilter();
ldapFilter.and(objectClassFilter).and(memberOfFilter);

List<LdapUser> users = ldapService.getUsers(AdDomain.AD_ENT, ldapFilter, true);

users.forEach(user -> {
    System.out.println("User: " + user.getFirstName() + " " + user.getLastName());
});


†********
public List<LdapUser> getUsers(final AdDomain domain, final Filter ldapFilter, boolean includeMgrEmpNum) {
    LdapSearch ldapSearch = getLdapSearch(domain);

    // Step 1: Execute original query to check if it's a group search
    List<String> memberDns = new ArrayList<>();

    this.search(domain, ldapFilter, new String[]{"member"}, (Attributes attrs) -> {
        Attribute members = attrs.get("member");
        if (members != null) {
            NamingEnumeration<?> all = members.getAll();
            while (all.hasMore()) {
                memberDns.add((String) all.next());
            }
        }
        return null;
    });

    Filter effectiveFilter;
    if (!memberDns.isEmpty()) {
        // Step 2: Build OR filter from member DNs
        OrFilter orFilter = new OrFilter();
        for (String dn : memberDns) {
            orFilter.or(new EqualsFilter("distinguishedName", dn));
        }
        effectiveFilter = orFilter;
    } else {
        // Not a group search or no members, use original filter
        effectiveFilter = ldapFilter;
    }

    // Step 3: Build and run final query
    LdapQuery query = ldapSearch.getBaseQuery(domain).filter(effectiveFilter);
    List<LdapUser> users = ldapSearch.search(query);

    // Step 4: Add manager info if requested
    if (includeMgrEmpNum) {
        users = users.stream().map(u -> {
            u.setManager(getManager(u, ldapSearch));
            return u;
        }).collect(Collectors.toList());
    }

    return users;
}

********"''"""""****

public List<LdapUser> getUsers(final AdDomain domain, final Filter ldapFilter, boolean includeMgrEmpNum) {
    LdapSearch ldapSearch = getLdapSearch(domain);

    // Step 1: Find all groups matching the filter (e.g., dtca_sptfm_*)
    List<String> memberDns = new ArrayList<>();

    this.search(domain, ldapFilter, new String[]{"member"}, (Attributes attrs) -> {
        Attribute members = attrs.get("member");
        if (members != null) {
            NamingEnumeration<?> all = members.getAll();
            while (all.hasMore()) {
                memberDns.add((String) all.next());
            }
        }
        return null;
    });

    // Step 2: Remove duplicates
    Set<String> uniqueDns = new HashSet<>(memberDns);

    // Step 3: Build OR filter for all user DNs
    Filter effectiveFilter;
    if (!uniqueDns.isEmpty()) {
        OrFilter orFilter = new OrFilter();
        for (String dn : uniqueDns) {
            orFilter.or(new EqualsFilter("distinguishedName", dn));
        }
        effectiveFilter = orFilter;
    } else {
        // No groups found or no members, fallback to original filter
        effectiveFilter = ldapFilter;
    }

    // Step 4: Search for users using the new filter
    LdapQuery query = ldapSearch.getBaseQuery(domain).filter(effectiveFilter);
    List<LdapUser> users = ldapSearch.search(query);

    // Step 5: Include manager details if requested
    if (includeMgrEmpNum) {
        users = users.stream().map(u -> {
            u.setManager(getManager(u, ldapSearch));
            return u;
        }).collect(Collectors.toList());
    }

    return users;
}


******REPLACING CODE HERE****
public Map<String, String> getUserIdsAndNamesFromLdap() {
    // Define the LDAP group name wildcard filter
    Filter groupFilter = new AndFilter()
        .and(new EqualsFilter("objectClass", "group"))
        .and(new LikeFilter("cn", "dtca_sptfm_*"));  // Change this pattern if needed

    // Call your updated getUsers() method
    List<LdapUser> users = getUsers(adDomain, groupFilter, false);

    // Convert to Map<username, "LastName,FirstName">
    Map<String, String> userMap = users.stream()
        .filter(user -> user.getUserName() != null && user.getFirstName() != null && user.getLastName() != null)
        .collect(Collectors.toMap(
            user -> user.getUserName().toLowerCase(),
            user -> user.getLastName() + "," + user.getFirstName(),
            (first, second) -> first // in case of duplicate usernames
        ));

    // Print the result for testing
    userMap.forEach((k, v) -> System.out.println(k + " -> " + v));

    return userMap;
}
