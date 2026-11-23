package internal.org.springframework.versions.jpa;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.IncorrectResultSizeDataAccessException;
import org.springframework.jdbc.CannotGetJdbcConnectionException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.rowset.SqlRowSet;

import java.security.Principal;
import java.util.Arrays;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

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
            @DisplayName("Given selecting a lock record fails")
            class SelectingLockRecordFails {
                @BeforeEach
                void setUp() {
                    jdbcTemplate = mock(JdbcTemplate.class);

                    entityId = "some-id";
                    principal = mock(Principal.class);
                    when(principal.getName()).thenReturn("some-principal");

                    when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), anyString(), anyString())).thenThrow(new CannotGetJdbcConnectionException("connection-error"));

                    locker = new JpaLockingServiceImpl(jdbcTemplate);

                    try {
                        result = locker.lock(entityId, principal);
                    } catch (Exception e) {
                        JpaLockingServiceImplTest.this.e = e;
                    }
                }

                @Test
                void shouldThrowDataAccessExceptionClass() {
                    assertThat(e).isInstanceOf(DataAccessException.class);
                    assertThat(e.getMessage()).isEqualTo("connection-error");
                }
            }

            @Nested
            @DisplayName("Given inserting the lock record fails")
            class InsertingLockRecordFails {
                @BeforeEach
                void setUp() {
                    jdbcTemplate = mock(JdbcTemplate.class);

                    entityId = "some-id";
                    principal = mock(Principal.class);
                    when(principal.getName()).thenReturn("some-principal");

                    when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), anyString(), anyString())).thenReturn(0);
                    when(jdbcTemplate.update(anyString(), anyString(), anyString())).thenThrow(new CannotGetJdbcConnectionException("connection-error"));

                    locker = new JpaLockingServiceImpl(jdbcTemplate);

                    try {
                        result = locker.lock(entityId, principal);
                    } catch (Exception e) {
                        JpaLockingServiceImplTest.this.e = e;
                    }
                }

                @Test
                void shouldThrowDataAccessExceptionClass() {
                    assertThat(e).isInstanceOf(DataAccessException.class);
                    assertThat(e.getMessage()).isEqualTo("connection-error");
                }
            }
        }

        @Nested
        class Unlock {
            @Nested
            @DisplayName("Given the lock record deletion fails")
            class LockRecordDeletionFails {
                @BeforeEach
                void setUp() {
                    jdbcTemplate = mock(JdbcTemplate.class);

                    entityId = "some-id";
                    principal = mock(Principal.class);
                    when(principal.getName()).thenReturn("some-principal");

                    when(jdbcTemplate.update(anyString(), anyString(), anyString())).thenThrow(new CannotGetJdbcConnectionException("connection-error"));

                    locker = new JpaLockingServiceImpl(jdbcTemplate);

                    try {
                        result = locker.unlock(entityId, principal);
                    } catch (Exception e) {
                        JpaLockingServiceImplTest.this.e = e;
                    }
                }

                @Test
                void shouldThrowDataAccessException() {
                    assertThat(e).isInstanceOf(DataAccessException.class);
                    assertThat(e.getMessage()).isEqualTo("connection-error");
                }
            }
        }

        @Nested
        class IsLockOwner {
            @Nested
            @DisplayName("Given a null principal")
            class NullPrincipal {
                @BeforeEach
                void setUp() {
                    jdbcTemplate = mock(JdbcTemplate.class);

                    entityId = "some-id";
                    principal = mock(Principal.class);
                    when(principal.getName()).thenReturn("some-principal");

                    principal = null;

                    locker = new JpaLockingServiceImpl(jdbcTemplate);

                    try {
                        result = locker.isLockOwner(entityId, null);
                    } catch (Exception e) {
                        JpaLockingServiceImplTest.this.e = e;
                    }
                }

                @Test
                void shouldThrowSecurityException() {
                    assertThat(e).isInstanceOf(SecurityException.class);
                }
            }

            @Nested
            @DisplayName("Given the database fails")
            class DatabaseFails {
                @BeforeEach
                void setUp() {
                    jdbcTemplate = mock(JdbcTemplate.class);

                    entityId = "some-id";
                    principal = mock(Principal.class);
                    when(principal.getName()).thenReturn("some-principal");

                    when(jdbcTemplate.queryForRowSet(anyString(), anyString(), anyString())).thenThrow(new CannotGetJdbcConnectionException("connection-error"));

                    locker = new JpaLockingServiceImpl(jdbcTemplate);

                    try {
                        result = locker.isLockOwner(entityId, principal);
                    } catch (Exception e) {
                        JpaLockingServiceImplTest.this.e = e;
                    }
                }

                @Test
                void shouldThrowDataAccessException() {
                    assertThat(e).isInstanceOf(DataAccessException.class);
                }
            }

            @Nested
            @DisplayName("Given the principal is the lock owner")
            class PrincipalIsLockOwner {
                @BeforeEach
                void setUp() {
                    jdbcTemplate = mock(JdbcTemplate.class);

                    entityId = "some-id";
                    principal = mock(Principal.class);
                    when(principal.getName()).thenReturn("some-principal");

                    SqlRowSet rs = mock(SqlRowSet.class);
                    when(rs.next()).thenReturn(true);
                    when(jdbcTemplate.queryForRowSet(anyString(), anyString(), anyString())).thenReturn(rs);

                    locker = new JpaLockingServiceImpl(jdbcTemplate);

                    try {
                        result = locker.isLockOwner(entityId, principal);
                    } catch (Exception e) {
                        JpaLockingServiceImplTest.this.e = e;
                    }
                }

                @Test
                void shouldReturnTrue() {
                    assertThat(result).isEqualTo(true);
                }
            }

            @Nested
            @DisplayName("Given the principal is not the lock owner")
            class PrincipalIsNotLockOwner {
                @BeforeEach
                void setUp() {
                    jdbcTemplate = mock(JdbcTemplate.class);

                    entityId = "some-id";
                    principal = mock(Principal.class);
                    when(principal.getName()).thenReturn("some-principal");

                    SqlRowSet rs = mock(SqlRowSet.class);
                    when(rs.next()).thenReturn(false);
                    when(jdbcTemplate.queryForRowSet(anyString(), anyString(), anyString())).thenReturn(rs);

                    locker = new JpaLockingServiceImpl(jdbcTemplate);

                    try {
                        result = locker.isLockOwner(entityId, principal);
                    } catch (Exception e) {
                        JpaLockingServiceImplTest.this.e = e;
                    }
                }

                @Test
                void shouldReturnFalse() {
                    assertThat(result).isEqualTo(false);
                }
            }
        }

        @Nested
        class LockOwner {
            @Nested
            @DisplayName("Given the database fails")
            class DatabaseFails {
                @BeforeEach
                void setUp() {
                    jdbcTemplate = mock(JdbcTemplate.class);

                    entityId = "some-id";

                    when(jdbcTemplate.query(anyString(), ArgumentMatchers.<RowMapper<String>>any())).thenThrow(new CannotGetJdbcConnectionException("connection-error"));

                    locker = new JpaLockingServiceImpl(jdbcTemplate);

                    try {
                        result = locker.lockOwner(entityId);
                    } catch (Exception e) {
                        JpaLockingServiceImplTest.this.e = e;
                    }
                }

                @Test
                void shouldThrowDataAccessException() {
                    assertThat(e).isInstanceOf(DataAccessException.class);
                }
            }

            @Nested
            @DisplayName("Given there is no lock record")
            class NoLockRecord {
                @BeforeEach
                void setUp() {
                    jdbcTemplate = mock(JdbcTemplate.class);

                    entityId = "some-id";

                    when(jdbcTemplate.query(anyString(), ArgumentMatchers.<RowMapper<String>>any())).thenReturn(null);

                    locker = new JpaLockingServiceImpl(jdbcTemplate);

                    try {
                        result = locker.lockOwner(entityId);
                    } catch (Exception e) {
                        JpaLockingServiceImplTest.this.e = e;
                    }
                }

                @Test
                void shouldReturnNull() {
                    assertThat(result).isNull();
                }
            }

            @Nested
            @DisplayName("Given there is a lock record")
            class LockRecord {
                @BeforeEach
                void setUp() {
                    jdbcTemplate = mock(JdbcTemplate.class);

                    entityId = "some-id";

                    when(jdbcTemplate.query(anyString(), ArgumentMatchers.<RowMapper<String>>any())).thenReturn(Collections.singletonList("some-principal"));

                    locker = new JpaLockingServiceImpl(jdbcTemplate);

                    try {
                        result = locker.lockOwner(entityId);
                    } catch (Exception e) {
                        JpaLockingServiceImplTest.this.e = e;
                    }
                }

                @Test
                void shouldReturnPrincipal() {
                    assertThat(result).isInstanceOf(Principal.class);
                    assertThat(((Principal) result).getName()).isEqualTo("some-principal");
                }
            }

            @Nested
            @DisplayName("Given there are multiple lock records")
            class MultipleLockRecords {
                @BeforeEach
                void setUp() {
                    jdbcTemplate = mock(JdbcTemplate.class);

                    entityId = "some-id";

                    when(jdbcTemplate.query(anyString(), ArgumentMatchers.<RowMapper<String>>any())).thenReturn(Arrays.asList(("some-principal"), "some-other-principal"));

                    locker = new JpaLockingServiceImpl(jdbcTemplate);

                    try {
                        result = locker.lockOwner(entityId);
                    } catch (Exception e) {
                        JpaLockingServiceImplTest.this.e = e;
                    }
                }

                @Test
                void shouldThrowIncorrectResultSizeException() {
                    assertThat(e).isInstanceOf(IncorrectResultSizeDataAccessException.class);
                }
            }
        }
    }
}
