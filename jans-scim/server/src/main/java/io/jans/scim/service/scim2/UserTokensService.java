package io.jans.scim.service.scim2;

import io.jans.model.token.TokenEntity;
import io.jans.orm.PersistenceEntryManager;
import io.jans.orm.search.filter.Filter;
import io.jans.scim.model.scim.Client;
import io.jans.scim.model.scim2.*;
import io.jans.scim.service.PersonService;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Collectors;

import org.apache.commons.codec.digest.*;
import org.slf4j.Logger;

import static java.nio.charset.StandardCharsets.UTF_8;

@ApplicationScoped
public class UserTokensService implements Serializable {
    
	private static final long serialVersionUID = -1948992380577056420L;

    private static final Path SALT_PATH = Paths.get("/etc/jans/conf/salt");
    private static final HmacAlgorithms HASH_ALG = HmacAlgorithms.HMAC_SHA_256;
    
    public static final String TOKENS_DN = "ou=tokens,o=jans";
    public static final String CLIENTS_DN = "ou=clients,o=jans";
    
    private String peopleDN;

	@Inject
	private PersonService personService;

	@Inject
	private Logger log;

	@Inject
	private PersistenceEntryManager entryManager;
	
	public List<TokenResource> getTokens(String inum) throws Exception {
	    
	    Filter filter = Filter.createEqualityFilter("jansUsrDN", personService.getDnForPerson(inum));
	    log.debug("Getting tokens associated to {}", inum);
	    
	    List<TokenEntity> entries = entryManager.findEntries(peopleDN, TokenEntity.class, filter);
	    int nEntries = entries.size();
	    log.debug("{} token entries found", nEntries);
	    
	    List<TokenResource> list = new ArrayList<>();
	    String sk = sharedKey();
	    
	    for (TokenEntity te : entries) {
	        TokenResource tr = new TokenResource();
	        tr.setIti(te.getDn());
	        
	        tr.setType(te.getTokenType());
	        tr.setIssuedAt(Optional.ofNullable(te.getCreationDate()).map(Date::getTime).orElse(-1L));
	        tr.setExpiresAt(Optional.ofNullable(te.getExpirationDate()).map(Date::getTime).orElse(-1L));
	        tr.setClientId(te.getClientId());
	        
	        tr.setHash(hash(te.getDn(), sk));
	        
	        String skopes = te.getScope();
	        if (skopes != null && skopes.length() > 0) {
	            tr.setScopes(Arrays.asList(skopes.split(" ")));
	        } else {
	            tr.setScopes(Collections.emptyList());
	        }
	        list.add(tr);
	    }
	    sk = null;
	    
	    if (nEntries > 0) {
            //The bellow avoids to make several DB queries and get all client names at once
            Set<String> clientIds = list.stream().map(TokenResource::getClientId)
                    .filter(Objects::nonNull).collect(Collectors.toSet());
                    
            Filter[] filters = clientIds.stream().map(id -> Filter.createEqualityFilter("inum", id))
                    .collect(Collectors.toList()).toArray(new Filter[0]);
                    
            List<Client> clients = entryManager.findEntries(CLIENTS_DN, Client.class,
                    Filter.createORFilter(filters));            
            log.debug("Tokens bound to {} different clients", clients.size());
            
            Map<String, String> clientNames = clients.stream().collect(
                    Collectors.toMap(c -> c.getInum(), c -> Optional.ofNullable(c.getDisplayName()).orElse("")));
            
            list.forEach(te -> te.setAppName(clientNames.get(te.getClientId()))); 
	    }
	    return list;
	    
	}
	
	public boolean revoke(String id) {
	    
	    TokenEntity te = entryManager.find(id, TokenEntity.class, new String[]{ "grtId" });
	    String grantId = te.getGrantId();
	    
	    List<TokenEntity> totems = entryManager.findEntries(TOKENS_DN, TokenEntity.class,
	            Filter.createEqualityFilter("grtId", grantId), new String[]{ "dn" });
	    log.debug("Removing tokens associated to grant ID {}", grantId);
	    
	    boolean success = true;
        for (TokenEntity totem : totems) {
            try {
                entryManager.remove(totem.getDn(), TokenEntity.class);
            } catch (Exception e) {
                success = false;
                log.error("Error removing token", e);
            }
        }
	    return success;
	    
	}
    
    private String hash(String message, String sharedKey) throws IOException {        
        return new HmacUtils(HASH_ALG, sharedKey).hmacHex(message);       
    }    
    
    private String sharedKey() throws IOException {
        
        //I preferred not to have file contents in memory (static var)
        Properties p = new Properties();
        p.load(new StringReader(Files.readString(SALT_PATH, UTF_8)));
        return p.getProperty("encodeSalt");
        
    }
	
	@PostConstruct
    private void init() {
        peopleDN = personService.getDnForPerson(null);
    }
    
}
