package service;

import dto.response.NoteResponse;
import dto.response.UserResponseDto;
import entity.Note;
import entity.User;
import exception.UserNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import repository.NoteRepository;
import repository.UserRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminService {
    private final UserRepository userRepository;
    private final NoteRepository noteRepository;

    @Transactional(readOnly = true)
    public List<UserResponseDto> getAllAccounts(Pageable pageable) {
        List<User> users = userRepository.findAll();

        return users.stream().map(user -> new UserResponseDto(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getRole(),
                user.getNoteCount()
        )).toList();
    }

    @Transactional(readOnly = true)
    public UserResponseDto getUserByUsername(String username) {
        User user = userRepository.findByUsername(username).orElseThrow(() -> new UserNotFoundException(username));

        return UserResponseDto.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .role(user.getRole())
                .noteCount(user.getNoteCount())
                .build();
    }

    @Transactional(readOnly = true)
    public Page<NoteResponse> getAllNotesByUser(String username, Pageable pageable) {
        User user = userRepository.findByUsername(username).orElseThrow(() -> new UserNotFoundException(username));
        Page<Note> notes = noteRepository.findAllByUserId(user.getId(), pageable);

        return notes.map(note -> NoteResponse.builder()
                .id(note.getId())
                .title(note.getTitle())
                .content(note.getContent())
                .createdAt(note.getCreatedAt())
                .updatedAt(note.getUpdatedAt())
                .build());
    }

    public void deleteNoteById(Long id) {
        noteRepository.deleteById(id);
    }

    public void deleteUserById(Long id) {
        userRepository.deleteById(id);
    }

    public void deleteUserByName(String name) {
        userRepository.deleteByUsername(name);
    }
}