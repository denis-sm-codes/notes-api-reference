package service;

import dto.request.CreateNoteRequest;
import dto.response.NoteResponse;
import entity.Note;
import entity.User;
import exception.NoteNotFoundException;
import exception.UserNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import repository.NoteRepository;
import repository.UserRepository;

import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class NoteServiceTest {

    @Mock
    private NoteRepository noteRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private NoteService noteService;

    @Test
    void createNote_Success_ReturnsNoteResponse() {
        Long userId = 1L;
        User user = User.builder()
                .id(userId)
                .username("Test_Name")
                .noteCount(0)
                .build();

        CreateNoteRequest request = new CreateNoteRequest("Test Title", "Test Content");

        Authentication authentication = mock(Authentication.class);
        SecurityContext securityContext = mock(SecurityContext.class);

        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(user);

        SecurityContextHolder.setContext(securityContext);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        Note savedNote = Note.builder()
                .id(100L)
                .title(request.title())
                .content(request.content())
                .user(user)
                .createdAt(ZonedDateTime.now())
                .updatedAt(ZonedDateTime.now())
                .build();

        when(noteRepository.save(any(Note.class))).thenReturn(savedNote);

        NoteResponse response = noteService.createNote(request);

        assertNotNull(response);
        assertEquals(100L, response.id());
        assertEquals("Заголовок", response.title());
        assertEquals("Текст заметки", response.content());
        assertEquals(1, user.getNoteCount()); // Проверяем, что счетчик заметок увеличился

        verify(noteRepository, times(1)).save(any(Note.class));
        verify(userRepository, times(1)).save(user);

        SecurityContextHolder.clearContext();
    }

    @Test
    void createNote_WhenUserNotFound_ThrowsUserNotFoundException() {
        Long userId = 1L;
        User user = User.builder().id(userId).username("Test_Name").build();
        CreateNoteRequest request = new CreateNoteRequest("Test Title", "Test Content");

        Authentication authentication = mock(Authentication.class);
        SecurityContext securityContext = mock(SecurityContext.class);

        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(user);
        SecurityContextHolder.setContext(securityContext);

        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class, () -> noteService.createNote(request));

        verify(noteRepository, never()).save(any(Note.class));

        SecurityContextHolder.clearContext();
    }

    @Test
    void getNoteById_Success_ReturnsNoteResponse(){
        Long userId = 1L;
        User user = User.builder().id(userId).username("Test_Name").build();

        Long noteId = 100L;
        Note noteFromDB = Note.builder()
                .id(noteId)
                .user(user)
                .title("Test Title")
                .content("Test Content")
                .createdAt(ZonedDateTime.now())
                .updatedAt(ZonedDateTime.now())
                .build();

        Authentication authentication = mock(Authentication.class);
        SecurityContext securityContext = mock(SecurityContext.class);

        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(user);

        SecurityContextHolder.setContext(securityContext);

        when(noteRepository.findByIdAndUserId(noteId, userId)).thenReturn(Optional.of(noteFromDB));

        NoteResponse noteResponse = noteService.getNoteById(noteId);

        assertNotNull(noteResponse);
        assertEquals(noteFromDB.getId(), noteResponse.id());
        assertEquals(noteFromDB.getTitle(), noteResponse.title());
        assertEquals(noteFromDB.getContent(), noteResponse.content());
        assertEquals(noteFromDB.getCreatedAt(), noteResponse.createdAt());
        assertEquals(noteFromDB.getUpdatedAt(), noteResponse.updatedAt());

        verify(noteRepository, times(1)).findByIdAndUserId(noteId, userId);

        SecurityContextHolder.clearContext();
    }

    @Test
    void getNoteById_WhenNoteNotFound_ThenThrowsNoteNotFoundException(){
        Long userId = 1L;
        User user = User.builder().id(userId).username("Test_Name").build();

        Long noteId = 100L;
        Note noteFromDB = Note.builder()
                .id(noteId)
                .user(user)
                .title("Test Title")
                .content("Test Content")
                .createdAt(ZonedDateTime.now())
                .updatedAt(ZonedDateTime.now())
                .build();

        Authentication authentication = mock(Authentication.class);
        SecurityContext securityContext = mock(SecurityContext.class);

        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(user);

        SecurityContextHolder.setContext(securityContext);

        when(noteRepository.findByIdAndUserId(noteId, userId)).thenReturn(Optional.empty());

        assertThrows(NoteNotFoundException.class, () -> noteService.getNoteById(noteId));

        verify(noteRepository, times(1)).findByIdAndUserId(noteId, userId);
        verifyNoMoreInteractions(noteRepository);

        SecurityContextHolder.clearContext();
    }
}