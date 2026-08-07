package exception;

import entity.Note;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class NoteNotFoundException extends RuntimeException {

    public NoteNotFoundException(String message) {
        super(message);
    }

    public NoteNotFoundException(Long id) {
        super("Note not found with id: " + id);
    }
}
