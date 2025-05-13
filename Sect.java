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
    public List<LdapUser> getUsersByGroup(final AdDomain domain, final String groupName, boolean includeMgrEmpNum) {
    LdapSearch ldapSearch = getLdapSearch(domain);

    // Step 1: Search for the group by CN
    Filter groupFilter = new EqualsFilter("cn", groupName);
    LdapQuery groupQuery = ldapSearch.getBaseQuery(domain).filter(groupFilter);

    List<String> memberDns = new ArrayList<>();

    ldapSearch.getLdapTemplate().search(groupQuery, (Attributes attrs) -> {
        Attribute members = attrs.get("member");
        if (members != null) {
            NamingEnumeration<?> allMembers = members.getAll();
            while (allMembers.hasMore()) {
                memberDns.add((String) allMembers.next());
            }
        }
        return null; // only extracting members
    });

    if (memberDns.isEmpty()) {
        return Collections.emptyList();
    }

    // Step 2: Build OR filter for all member DNs
    OrFilter orFilter = new OrFilter();
    for (String dn : memberDns) {
        orFilter.or(new EqualsFilter("distinguishedName", dn));
    }

    // Step 3: Search for all users matching these DNs
    LdapQuery userQuery = ldapSearch.getBaseQuery(domain).filter(orFilter);
    List<LdapUser> users = ldapSearch.search(userQuery);

    // Step 4: Set manager info if needed
    if (includeMgrEmpNum) {
        users = users.stream().map(u -> {
            u.setManager(getManager(u, ldapSearch));
            return u;
        }).collect(Collectors.toList());
    }

    return users;
}
