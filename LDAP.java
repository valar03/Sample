@Override
public List<LDAPUserInfoDto> getUserByGroup(String group) {
    List<LDAPUserInfoDto> adFinalList = new LinkedList<>();

    try {
        if (group.contains("*")) {
            // Wildcard handling
            String baseFilter = group.replace("*", ""); // e.g., DTCA_APD_SPFTM_

            // 1. Search for all group DNs with matching CNs
            List<String> groupDns = ldapTemplate.search(
                "OU=Groups,OU=DTC,OU=ENT,DC=ent,DC=wfb,DC=bank,DC=qa",
                "(cn=" + baseFilter + "*)",
                new SearchControls(),
                (attributes, name) -> "CN=" + attributes.get("cn").get() + "," + ldapSearchFilterBase
            );

            // 2. For each group, get users
            for (String groupDn : groupDns) {
                String filter = "(memberOf=" + groupDn + ")";
                List<LDAPUserInfoDto> groupUsers = getAllADUsers(filter, defaultPageSize);
                if (groupUsers != null) {
                    adFinalList.addAll(groupUsers.stream().filter(Objects::nonNull).collect(Collectors.toList()));
                }
            }
        } else {
            // Exact match handling (original logic)
            SearchControls searchControls = new SearchControls();
            searchControls.setSearchScope(SearchControls.SUBTREE_SCOPE);

            OrFilter searchFilter = new OrFilter();
            searchFilter.append(new EqualsFilter("memberOf", "CN=" + group + "," + ldapSearchFilterBase));
            System.out.println(searchFilter);

            List<LDAPUserInfoDto> adUserListGrp = getAllADUsers(searchFilter.encode(), defaultPageSize);
            if (adUserListGrp != null) {
                adFinalList = adUserListGrp.stream().filter(Objects::nonNull).collect(Collectors.toList());
            }
        }
    } catch (Exception e) {
        LOGGER.error("Error occurred in getUserByGroup method", e);
        e.printStackTrace();
    }

    return adFinalList;
}
