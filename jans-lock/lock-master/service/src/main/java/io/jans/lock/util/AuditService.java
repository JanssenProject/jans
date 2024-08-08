package io.jans.lock.util;

import io.jans.as.client.TokenRequest;
import io.jans.as.client.TokenResponse;
import io.jans.as.model.common.GrantType;
import io.jans.as.model.common.ScopeType;
import io.jans.as.model.uma.wrapper.Token;
import io.jans.as.model.util.Util;
import io.jans.lock.model.config.AppConfiguration;
import io.jans.model.net.HttpServiceResponse;
import io.jans.service.EncryptionService;
import io.jans.service.net.BaseHttpService;
import io.jans.util.security.StringEncrypter.EncryptionException;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.Invocation.Builder;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;

import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import org.apache.http.entity.ContentType;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.util.EntityUtils;

import org.apache.commons.lang3.time.DateUtils;

import org.json.JSONObject;
import org.slf4j.Logger;

@ApplicationScoped
public class AuditService {
    
    @Inject
    private Logger log;

    @Inject
    LockUtil lockUtil;
    
    

}
