package service;

import dto.request.CreateNoteRequest;
import dto.request.UpdateNoteRequest;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import repository.NoteRepository;
import repository.UserRepository;

import java.time.ZonedDateTime;
import java.util.List;
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
                .noteCount(0L)
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
        assertEquals("Test Title", response.title());
        assertEquals("Test Content", response.content());
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

    @Test
    void getAllUserNotes_Success_ReturnPageOfNoteResponses(){
        User user = User.builder().id(1L).username("Test Name").build();
        Pageable pageable = PageRequest.of(0, 10);

        Note note1 = Note.builder().id(101L).user(user).title("Test title1").content("Test Content1").build();
        Note note2 = Note.builder().id(102L).user(user).title("Test title2").content("Test Content2").build();
        List<Note> listNotes = List.of(note1, note2);

        Page<Note> pages = new PageImpl<>(listNotes, pageable, listNotes.size());

        Authentication authentication = mock(Authentication.class);
        SecurityContext securityContext = mock(SecurityContext.class);

        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(user);
        SecurityContextHolder.setContext(securityContext);

        when(noteRepository.findAllByUserId(user.getId(), pageable)).thenReturn(pages);

        Page<NoteResponse> resultPages = noteService.getAllUserNotes(pageable);

        assertNotNull(resultPages);
        assertEquals(2, resultPages.getTotalElements());
        assertEquals(0, resultPages.getNumber());
        assertEquals(10, resultPages.getSize());

        NoteResponse firstResponse = resultPages.getContent().get(0);
        assertEquals(note1.getId(), firstResponse.id());
        assertEquals(note1.getTitle(), firstResponse.title());

        verify(noteRepository, times(1)).findAllByUserId(user.getId(), pageable);
        verifyNoMoreInteractions(noteRepository);

        SecurityContextHolder.clearContext();
    }

    @Test
    void getAllUserNotes_WhenUserHasNoNotes_ReturnsEmptyPAge(){
        Long userId = 1L;
        User user = User.builder().id(userId).username("Test Name").build();
        Pageable pageable = PageRequest.of(0, 10);

        Page<Note> emptyPage = Page.empty(pageable);

        Authentication authentication = mock(Authentication.class);
        SecurityContext securityContext = mock(SecurityContext.class);

        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(user);
        SecurityContextHolder.setContext(securityContext);

        when(noteRepository.findAllByUserId(userId, pageable)).thenReturn(emptyPage);

        Page<NoteResponse> result = noteService.getAllUserNotes(pageable);

        assertNotNull(result);
        assertTrue(result.isEmpty());
        assertEquals(0, result.getTotalElements());

        verify(noteRepository, times(1)).findAllByUserId(userId,pageable);
        verifyNoMoreInteractions(noteRepository);

        SecurityContextHolder.clearContext();
    }

    @Test
    void updateNote_Success_ReturnsUpdatedNote(){
        Long noteId = 1L;
        Long userId = 2L;
        User user = User.builder().id(userId).username("Test Name").build();
        UpdateNoteRequest updateNoteRequest = new UpdateNoteRequest("New Title", "New Content");
        ZonedDateTime zonedDateTime = ZonedDateTime.now();

        Note note = Note.builder()
                .user(user)
                .id(noteId)
                .title("Old Title")
                .content("Old Content")
                .createdAt(zonedDateTime)
                .updatedAt(zonedDateTime)
                .build();

        Authentication authentication = mock(Authentication.class);
        SecurityContext securityContext = mock(SecurityContext.class);

        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(user);
        SecurityContextHolder.setContext(securityContext);

        when(noteRepository.findByIdAndUserId(noteId, userId)).thenReturn(Optional.of(note));

        NoteResponse noteResponse = noteService.updateNote(noteId, updateNoteRequest);

        assertNotNull(noteResponse);
        assertEquals(noteId, noteResponse.id());
        assertEquals(updateNoteRequest.title(), noteResponse.title());
        assertEquals(updateNoteRequest.content(), noteResponse.content());
        assertEquals(zonedDateTime, noteResponse.updatedAt());

        verify(noteRepository, times(1)).findByIdAndUserId(noteId, userId);
        verifyNoMoreInteractions(noteRepository);

        SecurityContextHolder.clearContext();
    }

    @Test
    void updateNote_WhenNoteNotFound_ThenThrowsNoteNotFoundException(){
        Long noteId = 1L;
        Long userId = 2L;
        User user = User.builder().id(userId).username("Test Name").build();
        UpdateNoteRequest updateNoteRequest = new UpdateNoteRequest("New Title", "New Content");

        Authentication authentication = mock(Authentication.class);
        SecurityContext securityContext = mock(SecurityContext.class);

        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(user);
        SecurityContextHolder.setContext(securityContext);

        when(noteRepository.findByIdAndUserId(noteId, userId)).thenReturn(Optional.empty());

        assertThrows(NoteNotFoundException.class, () -> noteService.updateNote(noteId, updateNoteRequest));

            verify(noteRepository, times(1)).findByIdAndUserId(noteId, userId);
            verifyNoMoreInteractions(noteRepository);

            SecurityContextHolder.clearContext();

    }

    @Test
    void deleteNote_Success_DecrementsNoteCountAndDeletesNote(){
        Long noteId = 1L;
        Long userId = 2L;
        Long initialNoteCount = 1L;
        User user = User.builder().id(userId).username("Test Name").noteCount(initialNoteCount).build();
        ZonedDateTime zonedDateTime = ZonedDateTime.now();

        Note note = Note.builder()
                .user(user)
                .id(noteId)
                .title("Test Title")
                .content("Test Content")
                .createdAt(zonedDateTime)
                .updatedAt(zonedDateTime)
                .build();

        Authentication authentication = mock(Authentication.class);
        SecurityContext securityContext = mock(SecurityContext.class);

        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(user);
        SecurityContextHolder.setContext(securityContext);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(noteRepository.findByIdAndUserId(noteId, userId)).thenReturn(Optional.of(note));

        noteService.deleteNote(noteId);
        Long newNoteCount = user.getNoteCount();

        assertEquals(0L, user.getNoteCount());
        assertEquals(newNoteCount, user.getNoteCount());

        verify(userRepository, times(1)).findById(userId);
        verify(noteRepository, times(1)).findByIdAndUserId(noteId, userId);
        verify(noteRepository, times(1)).delete(note);

        verifyNoMoreInteractions(userRepository, noteRepository);

        SecurityContextHolder.clearContext();
    }

    @Test
    void deleteNote_WhenUserNotFound_ThrowsUserNotFoundException(){
        Long noteId = 1L;
        Long userId = 1L;
        Long noteCount = 1L;
        User user = User.builder().id(userId).username("Test Name").noteCount(noteCount).build();

        ZonedDateTime zonedDateTime = ZonedDateTime.now();
        Note note = Note.builder()
                .user(user)
                .id(noteId)
                .title("Test Title")
                .content("Test Content")
                .createdAt(zonedDateTime)
                .updatedAt(zonedDateTime)
                .build();

        Authentication authentication = mock(Authentication.class);
        SecurityContext securityContext = mock(SecurityContext.class);

        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(user);
        SecurityContextHolder.setContext(securityContext);

        when(userRepository.findById(noteId)).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class, () -> noteService.deleteNote(noteId));

        verify(userRepository, times(1)).findById(userId);
        verifyNoMoreInteractions(userRepository);

        SecurityContextHolder.clearContext();
    }

    @Test
    void deleteNote_WhenNoteNotFound_ThrowsNoteNotFoundException(){
        Long noteId = 1L;
        Long userId = 1L;
        Long noteCount = 1L;
        User user = User.builder().id(userId).username("Test Name").noteCount(noteCount).build();

        ZonedDateTime zonedDateTime = ZonedDateTime.now();
        Note note = Note.builder()
                .user(user)
                .id(noteId)
                .title("Test Title")
                .content("Test Content")
                .createdAt(zonedDateTime)
                .updatedAt(zonedDateTime)
                .build();

        Authentication authentication = mock(Authentication.class);
        SecurityContext securityContext = mock(SecurityContext.class);

        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(user);
        SecurityContextHolder.setContext(securityContext);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(noteRepository.findByIdAndUserId(noteId, userId)).thenReturn(Optional.empty());

        assertThrows(NoteNotFoundException.class, () -> noteService.deleteNote(noteId));

        verify(userRepository, times(1)).findById(userId);
        verify(noteRepository, times(1)).findByIdAndUserId(noteId, userId);
        verifyNoMoreInteractions(userRepository, noteRepository);

        SecurityContextHolder.clearContext();
    }
}


















