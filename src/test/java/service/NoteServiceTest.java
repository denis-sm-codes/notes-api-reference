package service;

import dto.request.CreateNoteRequest;
import dto.response.NoteResponse;
import entity.Note;
import entity.User;
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
        // 1. ARRANGE (Подготовка данных и моков)
        Long userId = 1L;
        User user = User.builder()
                .id(userId)
                .username("denis")
                .noteCount(0)
                .build();

        CreateNoteRequest request = new CreateNoteRequest("Заголовок", "Текст заметки");

        // Мокаем SecurityContextHolder, чтобы метод смог достать текущего пользователя
        Authentication authentication = mock(Authentication.class);
        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(user);
        SecurityContextHolder.setContext(securityContext);

        // Мокаем работу с репозиториями
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

        // 2. ACT (Вызов тестируемого метода)
        NoteResponse response = noteService.createNote(request);

        // 3. ASSERT (Проверки)
        assertNotNull(response);
        assertEquals(100L, response.id());
        assertEquals("Заголовок", response.title());
        assertEquals("Текст заметки", response.content());
        assertEquals(1, user.getNoteCount()); // Проверяем, что счетчик заметок увеличился

        // Проверяем, что сохранения в БД действительно вызывались
        verify(noteRepository, times(1)).save(any(Note.class));
        verify(userRepository, times(1)).save(user);

        // Очищаем контекст безопасности после теста
        SecurityContextHolder.clearContext();
    }

    @Test
    void createNote_WhenUserNotFound_ThrowsUserNotFoundException() {
        // ARRANGE
        Long userId = 1L;
        User user = User.builder().id(userId).username("denis").build();
        CreateNoteRequest request = new CreateNoteRequest("Заголовок", "Текст");

        Authentication authentication = mock(Authentication.class);
        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(user);
        SecurityContextHolder.setContext(securityContext);

        // Пользователь НЕ найден в репозитории
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        // ACT & ASSERT (Ждем выброс исключения)
        assertThrows(UserNotFoundException.class, () -> noteService.createNote(request));

        // Убеждаемся, что заметку даже не пытались сохранить
        verify(noteRepository, never()).save(any(Note.class));

        SecurityContextHolder.clearContext();
    }
}