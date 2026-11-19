package io.jans.configapi.plugin.mgt.service;

import io.jans.as.common.model.common.User;
import io.jans.configapi.core.service.ConfigUserService;
import io.jans.configapi.plugin.mgt.model.user.UserPatchRequest;
import io.jans.configapi.service.auth.ConfigurationService;
import io.jans.model.GluuStatus;
import io.jans.orm.PersistenceEntryManager;
import io.jans.orm.exception.VersionMismatchException;
import io.jans.orm.model.base.CustomObjectAttribute;
import io.jans.as.model.config.StaticConfiguration;
import org.mockito.Answers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.slf4j.Logger;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.testng.Assert.*;

public class UserMgmtServiceTest {

    private static final String INUM = "a1b2";
    private static final String DN = "inum=" + INUM + ",ou=people,o=jans";

    @Mock
    private Logger logger;
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private StaticConfiguration staticConfiguration;
    @Mock
    private ConfigurationService configurationService;
    @Mock
    private PersistenceEntryManager persistenceEntryManager;
    @Mock
    private ConfigUserService userService;

    @InjectMocks
    private UserMgmtService userMgmtService;

    private AutoCloseable mocks;

    // stored entry: what the ORM CASes against
    private final AtomicReference<User> stored = new AtomicReference<>();
    private final List<User> writes = new ArrayList<>();

    @BeforeMethod
    public void setUp() {
        mocks = MockitoAnnotations.openMocks(this);
        writes.clear();
        when(configurationService.getPersistenceType()).thenReturn("sql");
        when(userService.getUserByDn(DN)).thenAnswer(inv -> copy(stored.get()));
        doAnswer(inv -> {
            User entry = inv.getArgument(0);
            User current = stored.get();
            if (!Objects.equals(entry.getVersion(), current.getVersion())) {
                throw new VersionMismatchException(entry.getDn(), entry.getVersion());
            }
            entry.setVersion(current.getVersion() + 1);
            User saved = copy(entry);
            writes.add(saved);
            stored.set(saved);
            return null;
        }).when(persistenceEntryManager).updateWithVersion(any());
    }

    @AfterMethod
    public void tearDown() throws Exception {
        mocks.close();
    }

    @Test
    public void patchReadBeforeLockKeepsAccountInactive() throws Exception {
        stored.set(user(GluuStatus.ACTIVE, 1L));
        User staleRead = copy(stored.get());
        // lock script writes after the PATCH read
        stored.set(user(GluuStatus.INACTIVE, 2L));
        when(userService.getUserByInum(INUM)).thenReturn(staleRead, copy(stored.get()));

        User result = userMgmtService.patchUser(INUM, patch("[{\"op\":\"replace\",\"path\":\"/userId\",\"value\":\"jdoe2\"}]"));

        assertEquals(writes.size(), 1);
        assertEquals(stored.get().getStatus(), GluuStatus.INACTIVE);
        assertEquals(stored.get().getUserId(), "jdoe2");
        assertEquals(stored.get().getVersion(), Long.valueOf(3L));
        assertEquals(result.getStatus(), GluuStatus.INACTIVE);
        verify(userService, times(2)).getUserByInum(INUM);
        verify(persistenceEntryManager, never()).merge(any());
    }

    @Test
    public void patchGivesUpAfterMaxAttempts() throws Exception {
        stored.set(user(GluuStatus.ACTIVE, 7L));
        when(userService.getUserByInum(INUM)).thenAnswer(inv -> user(GluuStatus.ACTIVE, 1L));

        assertThrows(VersionMismatchException.class, () -> userMgmtService.patchUser(INUM,
                patch("[{\"op\":\"replace\",\"path\":\"/userId\",\"value\":\"jdoe2\"}]")));

        assertTrue(writes.isEmpty());
        verify(userService, times(UserMgmtService.MAX_UPDATE_ATTEMPTS)).getUserByInum(INUM);
    }

    @Test
    public void updateRetriesOnFreshVersion() {
        stored.set(user(GluuStatus.INACTIVE, 2L));
        User replacement = user(GluuStatus.ACTIVE, 1L);
        replacement.setUserId("jdoe2");

        User result = userMgmtService.updateUser(replacement, false);

        assertEquals(writes.size(), 1);
        assertEquals(stored.get().getUserId(), "jdoe2");
        assertEquals(stored.get().getVersion(), Long.valueOf(3L));
        assertNotNull(result);
    }

    private static UserPatchRequest patch(String json) {
        UserPatchRequest request = new UserPatchRequest();
        request.setJsonPatchString(json);
        return request;
    }

    private static User user(GluuStatus status, Long version) {
        User user = new User();
        user.setDn(DN);
        user.setUserId("jdoe");
        user.setStatus(status);
        user.setVersion(version);
        List<CustomObjectAttribute> attributes = new ArrayList<>();
        attributes.add(new CustomObjectAttribute("inum", INUM));
        user.setCustomAttributes(attributes);
        return user;
    }

    private static User copy(User source) {
        User user = user(source.getStatus(), source.getVersion());
        user.setUserId(source.getUserId());
        return user;
    }
}
