package service;

import dto.response.NoteResponse;
import dto.response.UserResponseDto;
import entity.Note;
import entity.Role;
import entity.User;
import exception.NoteNotFoundException;
import exception.UserNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;
import repository.NoteRepository;
import repository.UserRepository;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
public class AdminServiceTest {

    @Mock
    private NoteRepository noteRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private AdminService adminService;

    @Test
    @DisplayName("Получить всех Пользователей Успех")
    void getAllAccounts_Success_ReturnsListOfResponseDto(){
        User user1 = User.builder()
                .id(1L)
                .username("Test Username1")
                .email("test1@email.com")
                .role(Role.ROLE_USER)
                .noteCount(5L)
                .build();
        User user2 = User.builder()
                .id(1L)
                .username("Test Username2")
                .email("test2@email.com")
                .role(Role.ROLE_USER)
                .noteCount(10L)
                .build();

        List<User> list = List.of(user1, user2);

        when(userRepository.findAll()).thenReturn(list);

        List<UserResponseDto> result = adminService.getAllAccounts();

        assertNotNull(result);
        assertEquals(2, result.size());

        UserResponseDto dto1 = result.get(0);
        assertEquals(user1.getId(), dto1.id());
        assertEquals(user1.getUsername(), dto1.username());
        assertEquals(user1.getEmail(), dto1.email());
        assertEquals(user1.getRole(), dto1.role());
        assertEquals(user1.getNoteCount(), dto1.noteCount());

        UserResponseDto dto2 = result.get(1);
        assertEquals(user2.getId(), dto2.id());
        assertEquals(user2.getUsername(), dto2.username());
        assertEquals(user2.getEmail(), dto2.email());
        assertEquals(user2.getRole(), dto2.role());
        assertEquals(user2.getNoteCount(), dto2.noteCount());

        verify(userRepository, times(1)).findAll();
        verifyNoMoreInteractions(userRepository);
    }

    @Test
    @DisplayName("Получить всех Пользователей Пустой Лист")
    void getAllAccounts_WhenNoUsers_ReturnsEmptyList() {
        when(userRepository.findAll()).thenReturn(Collections.emptyList());

        List<UserResponseDto> result = adminService.getAllAccounts();

        assertNotNull(result);
        assertTrue(result.isEmpty());

        verify(userRepository, times(1)).findAll();
        verifyNoMoreInteractions(userRepository);
    }

    @Test
    @DisplayName("Получить Пользователя по Имени Успех")
    void getUserByUsername_Success_ReturnsUserResponseDto(){
        User user = User.builder()
                .id(1L)
                .username("Test Username1")
                .email("test1@email.com")
                .role(Role.ROLE_USER)
                .noteCount(5L)
                .build();

        when(userRepository.findByUsername(user.getUsername())).thenReturn(Optional.of(user));

        UserResponseDto result = adminService.getUserByUsername(user.getUsername());

        assertNotNull(result);
        assertEquals(result.id(), user.getId());
        assertEquals(result.username(), user.getUsername());
        assertEquals(result.email(), user.getEmail());
        assertEquals(result.role(), user.getRole());
        assertEquals(result.noteCount(), user.getNoteCount());

        verify(userRepository, times(1)).findByUsername(user.getUsername());
        verifyNoMoreInteractions(userRepository);
    }

    @Test
    @DisplayName("Получить Пользователя по Имени Исключение")
    void getUserByUsername_WhenUserNotFound_ThrowsUserNotFoundException() {
        String request = "Test Name";

        when(userRepository.findByUsername(request)).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class, () -> adminService.getUserByUsername(request));

        verify(userRepository, times(1)).findByUsername(request);
        verifyNoMoreInteractions(userRepository);
    }

    @Test
    @DisplayName("Получить Заметки по Пользователю Успех")
    void getAllNotesByUser_Success_ReturnsPageOfNoteResponses(){
        String request = "Test Name1";

        User user = User.builder()
                .id(1L)
                .username("Test Name1")
                .email("test1@email.com")
                .role(Role.ROLE_USER)
                .noteCount(2L)
                .build();

        Note note1 = Note.builder().id(user.getId()).user(user).title("Test Tilte1").content("Test Content1").build();
        Note note2 = Note.builder().id(user.getId()).user(user).title("Test Tilte2").content("Test Content2").build();
        List<Note> listNotes = List.of(note1, note2);

        Pageable pageable = PageRequest.of(0, 10);
        Page<Note> pages = new PageImpl<>(listNotes, pageable, listNotes.size());

        when(userRepository.findByUsername(request)).thenReturn(Optional.of(user));
        when(noteRepository.findAllByUserId(user.getId(),pageable)).thenReturn(pages);

        Page<NoteResponse> result = adminService.getAllNotesByUser(request, pageable);

        assertNotNull(result);
        assertEquals(result.getTotalElements(), user.getNoteCount());
        assertEquals(result.getNumber(), 0);
        assertEquals(result.getSize(), 10);
        assertEquals(listNotes.size(), 2);

        verify(userRepository, times(1)).findByUsername(request);
        verifyNoMoreInteractions(userRepository);
    }

    @Test
    @DisplayName("Получить Заметки по Пользователю Исключение")
    void getAllNotesByUser_WhenUserNotFound_ThrowsUserNotFoundException() {
        String username = "Test Name";
        Pageable pageable = PageRequest.of(0, 10);

        when(userRepository.findByUsername(username)).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class, () -> adminService.getAllNotesByUser(username, pageable));

        verify(userRepository, times(1)).findByUsername(username);
        verifyNoMoreInteractions(userRepository);
        verifyNoInteractions(noteRepository);
    }

    @Test
    @DisplayName("Удалить Замету по ID Успех")
    void deleteNoteById_Success_DeletesNote(){
        Long noteId = 1L;

        when(noteRepository.existsById(noteId)).thenReturn(Boolean.TRUE);

        adminService.deleteNoteById(noteId);

        verify(noteRepository, times(1)).existsById(noteId);
        verify(noteRepository, times(1)).deleteById(noteId);
        verifyNoMoreInteractions(noteRepository);
    }

    @Test
    @DisplayName("Удалить Замету по ID Исключение")
    void deleteNoteById_WhenNoteNotFound_ThrowsNoteNotFoundException(){
        Long noteId = 1L;

        when(noteRepository.existsById(noteId)).thenReturn(Boolean.FALSE);
        assertThrows(NoteNotFoundException.class, () -> adminService.deleteNoteById(noteId));

        verify(noteRepository, times(1)).existsById(noteId);
        verify(noteRepository, never()).deleteById(noteId);
        verifyNoMoreInteractions(noteRepository);
    }

    @Test
    @DisplayName("Удалить Пользователя по ID Успех")
    void deleteUserById_Success_DeletesUser(){
        Long userId = 1L;

        when(userRepository.existsById(userId)).thenReturn(Boolean.TRUE);

        adminService.deleteUserById(userId);

        verify(userRepository, times(1)).existsById(userId);
        verify(userRepository, times(1)).deleteById(userId);
        verifyNoMoreInteractions(userRepository);
    }

    @Test
    @DisplayName("Удалить Пользователя по ID Исключение")
    void deleteUserById_WhenUserNotExist_ThrowsUserNotFoundException(){
        Long userId = 1L;

        when(userRepository.existsById(userId)).thenReturn(Boolean.FALSE);
        assertThrows(UserNotFoundException.class, () -> adminService.deleteUserById(userId));

        verify(userRepository, times(1)).existsById(userId);
        verify(userRepository, never()).deleteById(userId);
        verifyNoMoreInteractions(userRepository);
    }

    @Test
    @DisplayName("Удалить Пользователя по Имени Успех")
    void deleteUserByName_Success_DeletesUser(){
        String username = "Test Name";

        when(userRepository.existsByUsername(username)).thenReturn(Boolean.TRUE);

        adminService.deleteUserByName(username);

        verify(userRepository, times(1)).existsByUsername(username);
        verify(userRepository, times(1)).deleteByUsername(username);
        verifyNoMoreInteractions(userRepository);
    }

    @Test
    @DisplayName("Удалить Пользователя по Имени Исключение")
    void deleteUserByName_WhenUserNotExist_ThrowsUserNotFoundException(){
        String username = "Test Name";

        when(userRepository.existsByUsername(username)).thenReturn(Boolean.FALSE);
        assertThrows(UserNotFoundException.class, () -> adminService.deleteUserByName(username));

        verify(userRepository, times(1)).existsByUsername(username);
        verify(userRepository, never()).deleteByUsername(username);
        verifyNoMoreInteractions(userRepository);
    }

























}