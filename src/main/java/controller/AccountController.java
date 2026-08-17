package controller;

import dto.request.CreateNoteRequest;
import dto.request.UpdateNoteRequest;
import dto.response.NoteResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import security.AuthService;
import service.NoteService;

@RestController
@RequestMapping("/account")
@RequiredArgsConstructor
public class AccountController {

    private final NoteService noteService;
    private final AuthService authService;

    @PostMapping("/create_note")
    @ResponseStatus(HttpStatus.CREATED)
    public NoteResponse createNote(@Valid @RequestBody CreateNoteRequest request){
        return noteService.createNote(request);
    }

    @GetMapping("/get_note/{id}")
    public NoteResponse getNoteById(@PathVariable Long id){
        return noteService.getNoteById(id);
    }

    @GetMapping("/get_notes")
    public Page<NoteResponse> getAllUserNotes(
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable){

        return noteService.getAllUserNotes(pageable);
    }

    @PatchMapping("/update_note/{id}")
    public NoteResponse updateNote(@PathVariable Long id, @Valid @RequestBody UpdateNoteRequest request){
        return noteService.updateNote(id, request);
    }

    @DeleteMapping("/delete_note/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteNote(@PathVariable Long id){
        noteService.deleteNote(id);
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout() {
        authService.logout();
    }
}