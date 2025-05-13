String groupDn = "CN=" + groupNameStr + ",OU=Groups,DC=example,DC=com";

Filter memberOfFilter = new EqualsFilter("memberOf", groupDn);
Filter objectClassFilter = new EqualsFilter("objectClass", "user");

AndFilter ldapFilter = new AndFilter();
ldapFilter.and(objectClassFilter).and(memberOfFilter);

List<LdapUser> users = ldapService.getUsers(AdDomain.AD_ENT, ldapFilter, true);

users.forEach(user -> {
    System.out.println("User: " + user.getFirstName() + " " + user.getLastName());
});
