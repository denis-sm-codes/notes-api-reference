package controller;

import dto.response.NoteResponse;
import dto.response.UserResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import service.AdminService;

import java.util.List;

@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;

    @GetMapping("/get_accounts")
    public List<UserResponseDto> getAllAccounts(){
        return adminService.getAllAccounts();
    }

    @GetMapping("/get_user/{username}")
    public UserResponseDto getUserByUsername(@PathVariable String username){
        return adminService.getUserByUsername(username);
    }

    @GetMapping("/get_notes/{username}")
    public Page<NoteResponse> getAllNotesByUser(
            @PathVariable String username,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable){

        return adminService.getAllNotesByUser(username, pageable);
    }

    @DeleteMapping("/delete_note/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteNoteById(@PathVariable Long id){
        adminService.deleteNoteById(id);
    }

    @DeleteMapping("/delete_user/by_id/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteUserById(@PathVariable Long id){
        adminService.deleteUserById(id);
    }

    @DeleteMapping("/delete_user/by_username/{username}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteUserByName(@PathVariable String username){
        adminService.deleteUserByName(username);
    }
}