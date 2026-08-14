package service;

import dto.request.CreateNoteRequest;
import dto.request.UpdateNoteRequest;
import dto.response.NoteResponse;
import entity.Note;
import entity.User;
import exception.NoteNotFoundException;
import exception.UserNotFoundException;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import repository.NoteRepository;
import repository.UserRepository;

@Service
@AllArgsConstructor
public class NoteService {
    private final NoteRepository noteRepository;
    private final UserRepository userRepository;

    @Transactional
    public NoteResponse createNote(CreateNoteRequest request){
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        User principal = (User) authentication.getPrincipal();

        User user = userRepository.findById(principal.getId()).orElseThrow(() -> new UserNotFoundException(principal.getId()));

        Note note = Note.builder()
                .user(user)
                .title(request.title())
                .content(request.content())
                .build();
        Note savedNote = noteRepository.save(note);

        user.setNoteCount(user.getNoteCount() + 1);
        userRepository.save(user);

        return NoteResponse.builder()
                .id(savedNote.getId())
                .title(savedNote.getTitle())
                .content(savedNote.getContent())
                .createdAt(savedNote.getCreatedAt())
                .updatedAt(savedNote.getUpdatedAt())
                .build();
    }

    @Transactional(readOnly = true)
    public NoteResponse getNoteById(Long id){
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        User user = (User) authentication.getPrincipal();

        Note note = noteRepository.findByIdAndUserId(id, user.getId()).orElseThrow(() -> new NoteNotFoundException(id, user.getId()));

        return NoteResponse.builder()
                .id(note.getId())
                .title(note.getTitle())
                .content(note.getContent())
                .createdAt(note.getCreatedAt())
                .updatedAt(note.getUpdatedAt())
                .build();
    };

    @Transactional(readOnly = true)
    public Page<NoteResponse> getAllUserNotes(Pageable pageable){
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        User user = (User) authentication.getPrincipal();

        Page<Note> notes = noteRepository.findAllByUserId(user.getId(), pageable);

        return notes.map(note -> NoteResponse.builder()
                .id(note.getId())
                .title(note.getTitle())
                .content(note.getContent())
                .createdAt(note.getCreatedAt())
                .updatedAt(note.getUpdatedAt())
                .build());
    };

    @Transactional
    public NoteResponse updateNote(Long id, UpdateNoteRequest request){
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        User user = (User) authentication.getPrincipal();

        Note note = noteRepository.findByIdAndUserId(id, user.getId()).orElseThrow(() -> new NoteNotFoundException(id, user.getId()));
        note.setTitle(request.title());
        note.setContent(request.content());

        return NoteResponse.builder()
                .id(note.getId())
                .title(note.getTitle())
                .content(note.getContent())
                .createdAt(note.getCreatedAt())
                .updatedAt(note.getUpdatedAt())
                .build();
    };

    @Transactional
    public void deleteNote(Long id){
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        User principal = (User) authentication.getPrincipal();

        User user = userRepository.findById(principal.getId()).orElseThrow(() -> new UserNotFoundException(principal.getId()));

        Note note = noteRepository.findByIdAndUserId(id, user.getId()).orElseThrow(() -> new NoteNotFoundException(id, user.getId()));
        user.setNoteCount(user.getNoteCount() - 1);

        noteRepository.delete(note);
    };
}