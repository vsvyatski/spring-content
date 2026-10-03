package internal.org.springframework.versions.jpa;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import static org.assertj.core.api.Assertions.assertThat;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.security.Principal;
import java.sql.ResultSet;
import java.util.Arrays;
import java.util.Collections;

import org.mockito.ArgumentMatchers;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.IncorrectResultSizeDataAccessException;
import org.springframework.jdbc.CannotGetJdbcConnectionException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.rowset.SqlRowSet;

public class JpaLockingServiceImplTest {

    private JpaLockingServiceImpl locker;

    // mocks
    private JdbcTemplate jdbcTemplate;

    private Object entityId;
    private Principal principal;

    private Object result;
    private Exception e;

    
    @Nested
    class JpaLockingServiceImplCases {
        @Nested
        class Lock {
            @Nested
            class GivenSelectingALockRecordFails {
                @BeforeEach
                void setUp() throws Throwable {
                    jdbcTemplate = mock(JdbcTemplate.class);

                    entityId = "some-id";
                    principal = mock(Principal.class);
                    when(principal.getName()).thenReturn("some-principal");

                    when(jdbcTemplate.queryForObject(any(String.class), any(Object[].class), any(Class.class))).thenThrow(new CannotGetJdbcConnectionException("connection-error"));

                    locker = new JpaLockingServiceImpl(jdbcTemplate);

                    try {
                        result = locker.lock(entityId, principal);
                    } catch (Exception e) {
                        JpaLockingServiceImplTest.this.e = e;
                    }

                }

                @Test
                void shouldThrowTheDataAccessExceptionClass() throws Throwable {
                    assertThat(e).isInstanceOf(DataAccessException.class);
                    assertThat(e.getMessage()).isEqualTo("connection-error");

                }

            }

            @Nested
            class GivenInsertingTheLockRecordFails {
                @BeforeEach
                void setUp() throws Throwable {
                    jdbcTemplate = mock(JdbcTemplate.class);

                    entityId = "some-id";
                    principal = mock(Principal.class);
                    when(principal.getName()).thenReturn("some-principal");

                    ResultSet rs = mock(ResultSet.class);
                    when(jdbcTemplate.queryForObject(any(String.class), any(Object[].class), any(Class.class))).thenReturn(0);
                    when(jdbcTemplate.update(any(String.class), ArgumentMatchers.<String>any())).thenThrow(new CannotGetJdbcConnectionException("connection-error"));

                    locker = new JpaLockingServiceImpl(jdbcTemplate);

                    try {
                        result = locker.lock(entityId, principal);
                    } catch (Exception e) {
                        JpaLockingServiceImplTest.this.e = e;
                    }

                }

                @Test
                void shouldThrowTheDataAccessExceptionClass() throws Throwable {
                    assertThat(e).isInstanceOf(DataAccessException.class);
                    assertThat(e.getMessage()).isEqualTo("connection-error");

                }

            }

        }

        @Nested
        class Unlock {
            @Nested
            class GivenTheLockRecordDeletionFails {
                @BeforeEach
                void setUp() throws Throwable {
                    jdbcTemplate = mock(JdbcTemplate.class);

                    entityId = "some-id";
                    principal = mock(Principal.class);
                    when(principal.getName()).thenReturn("some-principal");

                    when(jdbcTemplate.update(any(String.class), ArgumentMatchers.<String>any())).thenThrow(new CannotGetJdbcConnectionException("connection-error"));

                    locker = new JpaLockingServiceImpl(jdbcTemplate);

                    try {
                        result = locker.unlock(entityId, principal);
                    } catch (Exception e) {
                        JpaLockingServiceImplTest.this.e = e;
                    }

                }

                @Test
                void shouldThrowADataAccessException() throws Throwable {
                    assertThat(e).isInstanceOf(DataAccessException.class);
                    assertThat(e.getMessage()).isEqualTo("connection-error");

                }

            }

        }

        @Nested
        class IsLockOwner {
            @Nested
            class GivenANullPrincipal {
                @BeforeEach
                void setUp() throws Throwable {
                    jdbcTemplate = mock(JdbcTemplate.class);

                    entityId = "some-id";
                    principal = mock(Principal.class);
                    when(principal.getName()).thenReturn("some-principal");

                    principal = null;

                    locker = new JpaLockingServiceImpl(jdbcTemplate);

                    try {
                        result = locker.isLockOwner(entityId, principal);
                    } catch (Exception e) {
                        JpaLockingServiceImplTest.this.e = e;
                    }

                }

                @Test
                void shouldThrowASecurityException() throws Throwable {
                    assertThat(e).isInstanceOf(SecurityException.class);

                }

            }

            @Nested
            class GivenTheDatabaseFails {
                @BeforeEach
                void setUp() throws Throwable {
                    jdbcTemplate = mock(JdbcTemplate.class);

                    entityId = "some-id";
                    principal = mock(Principal.class);
                    when(principal.getName()).thenReturn("some-principal");

                    when(jdbcTemplate.queryForRowSet(any(String.class), ArgumentMatchers.<String>any())).thenThrow(new CannotGetJdbcConnectionException("connection-error"));

                    locker = new JpaLockingServiceImpl(jdbcTemplate);

                    try {
                        result = locker.isLockOwner(entityId, principal);
                    } catch (Exception e) {
                        JpaLockingServiceImplTest.this.e = e;
                    }

                }

                @Test
                void shouldThrowTheDataAccessException() throws Throwable {
                    assertThat(e).isInstanceOf(DataAccessException.class);

                }

            }

            @Nested
            class GivenThePrincipalIsTheLockOwner {
                @BeforeEach
                void setUp() throws Throwable {
                    jdbcTemplate = mock(JdbcTemplate.class);

                    entityId = "some-id";
                    principal = mock(Principal.class);
                    when(principal.getName()).thenReturn("some-principal");

                    SqlRowSet rs = mock(SqlRowSet.class);
                    when(rs.next()).thenReturn(true);
                    when(jdbcTemplate.queryForRowSet(any(String.class), ArgumentMatchers.<String>any())).thenReturn(rs);

                    locker = new JpaLockingServiceImpl(jdbcTemplate);

                    try {
                        result = locker.isLockOwner(entityId, principal);
                    } catch (Exception e) {
                        JpaLockingServiceImplTest.this.e = e;
                    }

                }

                @Test
                void shouldReturnTrue() throws Throwable {
                    assertThat(result).isEqualTo(true);

                }

            }

            @Nested
            class GivenThePrincipalIsNotTheLockOwner {
                @BeforeEach
                void setUp() throws Throwable {
                    jdbcTemplate = mock(JdbcTemplate.class);

                    entityId = "some-id";
                    principal = mock(Principal.class);
                    when(principal.getName()).thenReturn("some-principal");

                    SqlRowSet rs = mock(SqlRowSet.class);
                    when(rs.next()).thenReturn(false);
                    when(jdbcTemplate.queryForRowSet(any(String.class), ArgumentMatchers.<String>any())).thenReturn(rs);

                    locker = new JpaLockingServiceImpl(jdbcTemplate);

                    try {
                        result = locker.isLockOwner(entityId, principal);
                    } catch (Exception e) {
                        JpaLockingServiceImplTest.this.e = e;
                    }

                }

                @Test
                void shouldReturnFalse() throws Throwable {
                    assertThat(result).isEqualTo(false);

                }

            }

        }

        @Nested
        class LockOwner {
            @Nested
            class GivenTheDatabaseFails {
                @BeforeEach
                void setUp() throws Throwable {
                    jdbcTemplate = mock(JdbcTemplate.class);

                    entityId = "some-id";

                    when(jdbcTemplate.query(anyString(), (RowMapper)any())).thenThrow(new CannotGetJdbcConnectionException("connection-error"));

                    locker = new JpaLockingServiceImpl(jdbcTemplate);

                    try {
                        result = locker.lockOwner(entityId);
                    } catch (Exception e) {
                        JpaLockingServiceImplTest.this.e = e;
                    }

                }

                @Test
                void shouldThrowTheDataAccessException() throws Throwable {
                    assertThat(e).isInstanceOf(DataAccessException.class);

                }

            }

            @Nested
            class GivenThereIsNoLockRecord {
                @BeforeEach
                void setUp() throws Throwable {
                    jdbcTemplate = mock(JdbcTemplate.class);

                    entityId = "some-id";

                    when(jdbcTemplate.query(anyString(), (RowMapper)any())).thenReturn(null);

                    locker = new JpaLockingServiceImpl(jdbcTemplate);

                    try {
                        result = locker.lockOwner(entityId);
                    } catch (Exception e) {
                        JpaLockingServiceImplTest.this.e = e;
                    }

                }

                @Test
                void shouldReturnNull() throws Throwable {
                    assertThat(result).isNull();

                }

            }

            @Nested
            class GivenThereIsALockRecord {
                @BeforeEach
                void setUp() throws Throwable {
                    jdbcTemplate = mock(JdbcTemplate.class);

                    entityId = "some-id";

                    when(jdbcTemplate.query(anyString(), (RowMapper)any())).thenReturn(Collections.singletonList("some-principal"));

                    locker = new JpaLockingServiceImpl(jdbcTemplate);

                    try {
                        result = locker.lockOwner(entityId);
                    } catch (Exception e) {
                        JpaLockingServiceImplTest.this.e = e;
                    }

                }

                @Test
                void shouldReturnAPrincipal() throws Throwable {
                    assertThat(result).isInstanceOf(Principal.class);
                    assertThat(((Principal)result).getName()).isEqualTo("some-principal");

                }

            }

            @Nested
            class GivenThereAreMulitpleLockRecords {
                @BeforeEach
                void setUp() throws Throwable {
                    jdbcTemplate = mock(JdbcTemplate.class);

                    entityId = "some-id";

                    when(jdbcTemplate.query(anyString(), (RowMapper)any())).thenReturn(Arrays.asList(new String[]{("some-principal"), "some-other-principal"}));

                    locker = new JpaLockingServiceImpl(jdbcTemplate);

                    try {
                        result = locker.lockOwner(entityId);
                    } catch (Exception e) {
                        JpaLockingServiceImplTest.this.e = e;
                    }

                }

                @Test
                void shouldThrowAnIncorrectResultSizeException() throws Throwable {
                    assertThat(e).isInstanceOf(IncorrectResultSizeDataAccessException.class);

                }

            }

        }

    }

}
