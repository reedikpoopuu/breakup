package com.example.demo.auth;

import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit-level coverage of the reconcile behaviour itself; {@link AdminBootstrapFailClosedTest}
 * covers the full-context "nothing configured" case.
 */
class AdminBootstrapRunnerTest {

    private final AppUserRepository repository = mock(AppUserRepository.class);

    @Test
    void seedsARowForEachUnknownIdentityInTheList() {
        when(repository.findBySmartIdIdentity(any())).thenReturn(Optional.empty());
        var runner = new AdminBootstrapRunner(repository, "EE-40504040001, EE-30303039914", "Admin");

        runner.run();

        var captor = org.mockito.ArgumentCaptor.forClass(AppUser.class);
        verify(repository, org.mockito.Mockito.times(2)).save(captor.capture());
        assertThat(captor.getAllValues())
                .extracting(AppUser::getSmartIdIdentity, AppUser::getRole)
                .containsExactlyInAnyOrder(
                        org.assertj.core.groups.Tuple.tuple("EE-40504040001", Role.ADMIN),
                        org.assertj.core.groups.Tuple.tuple("EE-30303039914", Role.ADMIN));
    }

    @Test
    void promotesAnExistingUserRowInsteadOfCreatingADuplicate() {
        AppUser existing = new AppUser("EE-40504040001", "OK TEST", Role.USER);
        when(repository.findBySmartIdIdentity("EE-40504040001")).thenReturn(Optional.of(existing));
        var runner = new AdminBootstrapRunner(repository, "EE-40504040001", "Admin");

        runner.run();

        verify(repository).save(existing);
        assertThat(existing.getRole()).isEqualTo(Role.ADMIN);
    }

    @Test
    void leavesAnExistingAdminRowUntouched() {
        AppUser existing = new AppUser("EE-40504040001", "Reedik Poopuu", Role.ADMIN);
        when(repository.findBySmartIdIdentity("EE-40504040001")).thenReturn(Optional.of(existing));
        var runner = new AdminBootstrapRunner(repository, "EE-40504040001", "Admin");

        runner.run();

        verify(repository, never()).save(any());
    }

    @Test
    void refusesToStartOnTheSemanticsIdentifierWireFormat() {
        var runner = new AdminBootstrapRunner(repository, "PNOEE-30303039914", "Admin");

        assertThatThrownBy(runner::run).isInstanceOf(IllegalStateException.class);

        verify(repository, never()).save(any());
    }

    @Test
    void doesNothingWhenNoIdentityIsConfiguredAndNoAdminRowsExist() {
        when(repository.findByRole(Role.ADMIN)).thenReturn(java.util.List.of());
        var runner = new AdminBootstrapRunner(repository, "", "Admin");

        runner.run();

        verify(repository, never()).save(any());
    }

    @Test
    void demotesAnExistingAdminNoLongerInTheConfiguredList() {
        AppUser offboarded = new AppUser("EE-99999999999", "Former Admin", Role.ADMIN);
        AppUser current = new AppUser("EE-40504040001", "Current Admin", Role.ADMIN);
        when(repository.findBySmartIdIdentity("EE-40504040001")).thenReturn(Optional.of(current));
        when(repository.findByRole(Role.ADMIN)).thenReturn(java.util.List.of(offboarded, current));
        var runner = new AdminBootstrapRunner(repository, "EE-40504040001", "Admin");

        runner.run();

        assertThat(offboarded.getRole()).isEqualTo(Role.USER);
        assertThat(current.getRole()).isEqualTo(Role.ADMIN);
        verify(repository).save(offboarded);
    }

    @Test
    void demotesEveryExistingAdminWhenTheConfiguredListGoesEmpty() {
        AppUser existingAdmin = new AppUser("EE-40504040001", "Reedik Poopuu", Role.ADMIN);
        when(repository.findByRole(Role.ADMIN)).thenReturn(java.util.List.of(existingAdmin));
        var runner = new AdminBootstrapRunner(repository, "", "Admin");

        runner.run();

        assertThat(existingAdmin.getRole()).isEqualTo(Role.USER);
        verify(repository).save(existingAdmin);
    }
}
