package org.sagebionetworks.bridge.spring.util;

import static com.google.common.net.HttpHeaders.IF_NONE_MATCH;
import static org.sagebionetworks.bridge.BridgeConstants.SESSION_TOKEN_HEADER;

import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.codec.binary.Hex;
import org.apache.commons.codec.digest.DigestUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.sagebionetworks.bridge.BridgeUtils;
import org.sagebionetworks.bridge.cache.CacheKey;
import org.sagebionetworks.bridge.cache.CacheProvider;
import org.sagebionetworks.bridge.exceptions.NotAuthenticatedException;
import org.sagebionetworks.bridge.models.accounts.UserSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import com.google.common.net.HttpHeaders;

/**
 * An etag is calculated from the ISO 8601 UTC modifiedOn timestamps that are specified in the 
 * @EtagSupport annotation. Each @EtagCacheKey entry describes how to produce a CacheKey by
 * substituting in key values (appId, studyId, userId) into the key (if there is no timestamp
 * under that key, it is considered a cache miss and no value will be returned for the entire
 * etag). This means that anywhere the entity described by a @EtagCacheKey is created, updated, 
 * or deleted, its associated CacheKey.etag must be added, updated, or removed with a 
 * Joda DateTime value. 
 */
@Aspect
@Component
public class EtagComponent {
    private static final Logger LOG = LoggerFactory.getLogger(EtagComponent.class);
    
    private static final String NO_VALUE_ERROR = "EtagSupport: no value for key: ";
    private static final String ORG_ID_FIELD = "orgId";
    private static final String USER_ID_FIELD = "userId";
    private static final String APP_ID_FIELD = "appId";
    
    private CacheProvider cacheProvider;
    
    private DigestUtils md5DigestUtils;
    
    @Autowired
    final void setCacheProvider(CacheProvider cacheProvider) {
        this.cacheProvider = cacheProvider;
    }
    
    @Autowired
    final void setDigestUtils(DigestUtils md5DigestUtils) {
        this.md5DigestUtils = md5DigestUtils;
    }

    protected HttpServletRequest request() {
        return ((ServletRequestAttributes) RequestContextHolder
                .currentRequestAttributes()).getRequest();
    }
    
    protected HttpServletResponse response() {
        return ((ServletRequestAttributes) RequestContextHolder
                .currentRequestAttributes()).getResponse();
    }
    
    protected EtagContext context(ProceedingJoinPoint joinPoint) {
        return new EtagContext(joinPoint);
    }
    
    @Around("@annotation(EtagSupport)")
    public Object checkEtag(ProceedingJoinPoint joinPoint) throws Throwable {
        EtagContext context = context(joinPoint);
        HttpServletResponse response = response();
        HttpServletRequest request = request();
        
        String requestEtag = request.getHeader(IF_NONE_MATCH);
        String sessionToken = request.getHeader(SESSION_TOKEN_HEADER);
        
        // Because this tag executes before security checks, it requires that the caller be 
        // authenticated. We can add a flag if we want to use this code on public endpoints 
        // to skip a check of the session.
        UserSession session = cacheProvider.getUserSession(sessionToken);
        if (session == null) {
            throw new NotAuthenticatedException();
        }

        // Etag can be null (until all dependent objects have cached their timestamps, 
        // or when a dependent object is deleted).
        String etag = calculateEtag(context, session);
        
        if (requestEtag != null) {
            if (requestEtag.equals(etag)) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Returning 304 for etag: " + etag);
                }
                response.addHeader(HttpHeaders.ETAG, etag);
                response.setStatus(304);
                return null;
            }
        }
        Object retValue = joinPoint.proceed();
        if (etag != null) {
            response.addHeader(HttpHeaders.ETAG, etag);
            if (LOG.isDebugEnabled()) {
                LOG.debug("Returning etag to response: " + etag);
            }
        }
        return retValue;
    }

    private String calculateEtag(EtagContext context, UserSession session) {
        // Collect timestamps for all the dependencies that determine freshness of etag
        List<DateTime> timestamps = new ArrayList<>();
        for (EtagCacheKey timestampKey : context.getCacheKeys()) {
            int len = timestampKey.keys().length;
            String[] resolvedKeyValues = new String[len];
            for (int i=0; i < len; i++) {
                // given getValue's behavior, this value will not be null;
                resolvedKeyValues[i] = getValue(context, session, timestampKey.keys()[i]);
            }
            // Retrieve the timestamp under this key
            CacheKey cacheKey = CacheKey.etag(timestampKey.model(), resolvedKeyValues);
            DateTime timestamp = cacheProvider.getObject(cacheKey, DateTime.class);
            if (timestamp == null) {
                return null; // this is a cache miss, any miss means there is no etag
            }
            timestamps.add(timestamp.withZone(DateTimeZone.UTC));
        }
        String base = BridgeUtils.SPACE_JOINER.join(timestamps);
        byte[] md5 = md5DigestUtils.digest(base.getBytes());
        return Hex.encodeHexString(md5);
    }
    
    private String getValue(EtagContext context, UserSession session, String fieldName) {
        if (context.getArgValues().containsKey(fieldName)) {
            String value = (String) context.getArgValues().get(fieldName);
            if (value == null) {
                throw new IllegalArgumentException(NO_VALUE_ERROR + fieldName);
            }
            return value;
        }
        String value = null;
        switch(fieldName) {
        case APP_ID_FIELD:
            value = session.getAppId();
            break;
        case USER_ID_FIELD:
            value = session.getId();
            break;
        case ORG_ID_FIELD:
            value = session.getParticipant().getOrgMembership();
            break;
        }
        if (value == null) {
            throw new IllegalArgumentException(NO_VALUE_ERROR + fieldName);
        }
        return value;
    }
}