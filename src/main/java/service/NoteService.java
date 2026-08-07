package service;

import dto.request.CreateNoteRequest;
import dto.request.UpdateNoteRequest;
import dto.response.NoteResponse;
import entity.Note;
import entity.User;
import exception.NoteNotFoundException;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import repository.NoteRepository;
import repository.UserRepository;

import java.util.Optional;

@Service
@AllArgsConstructor
public class NoteService {
    private final NoteRepository noteRepository;
    private final UserRepository userRepository;

    @Transactional
    public NoteResponse createNote(CreateNoteRequest request){
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        User user = (User) authentication.getPrincipal();

        Note note = Note.builder()
                .user(user)
                .title(request.title())
                .content(request.content())
                .build();
        Note savedNote = noteRepository.save(note);

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

        Note note = noteRepository.findByIdAndUserId(id, user.getId()).orElseThrow(() -> new NoteNotFoundException(id));

        return NoteResponse.builder()
                .id(note.getId())
                .title(note.getTitle())
                .content(note.getContent())
                .createdAt(note.getCreatedAt())
                .updatedAt(note.getUpdatedAt())
                .build();
    };

//    public Page<NoteResponse> getAllUserNotes(Pageable pageable){};
//
//    public NoteResponse updateNote(Long id, UpdateNoteRequest request){};
//
//    public void deleteNote(Long id){};


}
