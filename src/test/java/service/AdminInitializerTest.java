package service;

import entity.Role;
import entity.User;
import initializer.AdminInitializer;
import initializer.AdminProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import repository.UserRepository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminInitializerTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AdminProperties adminProperties;

    @InjectMocks
    private AdminInitializer adminInitializer;

    @Test
    void run_WhenAdminDoesNotExist_CreatesAdminSuccessfully() throws Exception {
        when(userRepository.existsByUsername("admin")).thenReturn(false);
        when(adminProperties.getUsername()).thenReturn("admin");
        when(adminProperties.getEmail()).thenReturn("admin@email.com");
        when(adminProperties.getPassword()).thenReturn("secret123");
        when(passwordEncoder.encode("secret123")).thenReturn("encoded_secret123");

        adminInitializer.run();

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);

        verify(userRepository, times(1)).existsByUsername("admin");
        verify(userRepository, times(1)).save(userCaptor.capture());
        verifyNoMoreInteractions(userRepository);

        User savedUser = userCaptor.getValue();
        assertEquals("admin", savedUser.getUsername());
        assertEquals("admin@email.com", savedUser.getEmail());
        assertEquals("encoded_secret123", savedUser.getPassword());
        assertEquals(Role.ROLE_ADMIN, savedUser.getRole());
    }

    @Test
    void run_WhenAdminAlreadyExists_DoesNotCreateAdmin() throws Exception {
        when(userRepository.existsByUsername("admin")).thenReturn(true);

        adminInitializer.run();

        verify(userRepository, times(1)).existsByUsername("admin");
        verify(userRepository, never()).save(any(User.class));
        verifyNoMoreInteractions(userRepository);
    }
}